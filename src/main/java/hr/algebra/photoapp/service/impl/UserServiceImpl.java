package hr.algebra.photoapp.service.impl;

import hr.algebra.photoapp.metrics.PhotoAppMetrics;
import hr.algebra.photoapp.model.PackageChange;
import hr.algebra.photoapp.model.PackageType;
import hr.algebra.photoapp.model.Role;
import hr.algebra.photoapp.model.User;
import hr.algebra.photoapp.util.FunctionalPhotoHelpers;
import hr.algebra.photoapp.repository.PackageChangeRepository;
import hr.algebra.photoapp.repository.PhotoRepository;
import hr.algebra.photoapp.repository.UserRepository;
import hr.algebra.photoapp.service.UserActionService;
import hr.algebra.photoapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Service Layer Pattern + Facade Pattern + Singleton Pattern
// Implements user management business logic, coordinating multiple repositories
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PhotoRepository photoRepository;
    private final PackageChangeRepository packageChangeRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserActionService userActionService;
    private final PhotoAppMetrics photoAppMetrics;


    @Override
    public User registerUser(String username, String email, String password, PackageType packageType) {
        if (existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        if (existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.USER)
                .packageType(packageType != null ? packageType : PackageType.FREE)
                .uploadsToday(0)
                .totalUploadsSize(0L)
                .registeredAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        
        // Log registration
        userActionService.logAction(savedUser, "REGISTER", 
                "User registered with package: " + savedUser.getPackageType(), null);

        photoAppMetrics.recordRegistration();
        
        return savedUser;
    }

    @Override
    public User registerUser(User user) {
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }
        
        if (user.getPackageType() == null) {
            user.setPackageType(PackageType.FREE);
        }
        
        if (user.getRegisteredAt() == null) {
            user.setRegisteredAt(LocalDateTime.now());
        }

        User savedUser = userRepository.save(user);
        
        userActionService.logAction(savedUser, "REGISTER", 
                "User registered (OAuth/Firebase)", null);
        
        return savedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "registeredAt"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findAllUsers(int page, int size) {
        return userRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "registeredAt"))
        );
    }

    @Override
    public void changePackage(String username, PackageType packageType) {
        User user = findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        PackageType oldPackage = user.getPackageType();
        user.setPackageType(packageType);
        user.setLastPackageChangeDate(LocalDate.now());
        userRepository.save(user);
        
        userActionService.logAction(user, "PACKAGE_CHANGE", 
                String.format("Package changed from %s to %s", oldPackage, packageType), null);
    }

    @Override
    public void requestPackageChange(String username, PackageType newPackage) {
        User user = findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (!user.canChangePackageToday()) {
            throw new IllegalStateException("You can only change package once per day");
        }

        PackageChange packageChange = PackageChange.builder()
                .user(user)
                .oldPackage(user.getPackageType())
                .newPackage(newPackage)
                .requestDate(LocalDate.now())
                .effectiveDate(LocalDate.now().plusDays(1))
                .applied(false)
                .build();

        packageChangeRepository.save(packageChange);
        user.setLastPackageChangeDate(LocalDate.now());
        userRepository.save(user);
        
        userActionService.logAction(user, "PACKAGE_CHANGE_REQUEST", 
                String.format("Requested package change from %s to %s (effective tomorrow)", 
                        user.getPackageType(), newPackage), null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canChangePackage(String username) {
        User user = findByUsername(username);
        return user != null && user.canChangePackageToday();
    }

    @Override
    public void updateUserProfile(Long userId, String email, PackageType packageType) {
        User user = findById(userId);
        
        if (email != null && !email.equals(user.getEmail())) {
            if (existsByEmail(email)) {
                throw new IllegalArgumentException("Email already in use");
            }
            user.setEmail(email);
        }
        
        if (packageType != null && packageType != user.getPackageType()) {
            PackageType oldPackage = user.getPackageType();
            user.setPackageType(packageType);
            userActionService.logAction(user, "ADMIN_PACKAGE_CHANGE", 
                    String.format("Admin changed package from %s to %s", oldPackage, packageType), null);
        }
        
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStatistics(String username) {
        User user = findByUsername(username);
        if (user == null) {
            return new HashMap<>();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("username", user.getUsername());
        stats.put("email", user.getEmail());
        stats.put("packageType", user.getPackageType());
        stats.put("uploadsToday", user.getUploadsToday());
        stats.put("maxUploadsPerDay", user.getPackageType().getMaxUploadsPerDay());
        stats.put("totalUploadsSize", user.getTotalUploadsSize());
        stats.put("registeredAt", user.getRegisteredAt());
        
        // Photo statistics
        long photoCount = photoRepository.countByOwner(user);
        Long totalPhotoSize = photoRepository.sumSizeByOwner(user);
        
        stats.put("totalPhotos", photoCount);
        stats.put("totalPhotosSizeMB", totalPhotoSize != null ? 
                String.format("%.2f MB", totalPhotoSize / (1024.0 * 1024.0)) : "0 MB");
        
        // Action count
        long actionCount = userActionService.getActionCount(username);
        stats.put("totalActions", actionCount);
        stats.put("actionsByType", FunctionalPhotoHelpers.countActionsByType(
                userActionService.getUserActions(username)));

        return stats;
    }

    @Override
    public void applyPendingPackageChanges() {
        List<PackageChange> pendingChanges = 
                packageChangeRepository.findByEffectiveDateAndAppliedFalse(LocalDate.now());

        pendingChanges.forEach(change -> {
            User user = change.getUser();
            user.setPackageType(change.getNewPackage());
            userRepository.save(user);

            change.setApplied(true);
            packageChangeRepository.save(change);

            userActionService.logAction(user, "PACKAGE_CHANGE_APPLIED",
                    String.format("Package changed from %s to %s",
                            change.getOldPackage(), change.getNewPackage()), null);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Override
    @Transactional
    public void deleteUserByAdmin(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (user.getRole() == Role.ADMIN) {
            throw new IllegalStateException("Cannot delete ADMIN users");
        }

        String currentUsername = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        if (user.getUsername().equals(currentUsername)) {
            throw new IllegalStateException("You cannot delete yourself");
        }

        // Delete:

        // Likes
        photoRepository.deleteLikesByUserId(user.getId());

        // User actions
        userActionService.deleteAllByUser(user);

        // Package changes
        packageChangeRepository.deleteByUser(user);

        // Photos
        photoRepository.deleteAllByOwner(user);

        // User
        userRepository.delete(user);
    }





}
