package hr.algebra.photoapp.service;

import hr.algebra.photoapp.model.PackageType;
import hr.algebra.photoapp.model.User;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

// Service Layer Pattern
// Business logic for user management
public interface UserService {

    User registerUser(String username, String email, String password, PackageType packageType);
    
    User registerUser(User user);

    User findByUsername(String username);
    
    User findById(Long id);
    
    List<User> findAllUsers();
    
    Page<User> findAllUsers(int page, int size);

    void changePackage(String username, PackageType packageType);
    
    void requestPackageChange(String username, PackageType newPackage);
    
    boolean canChangePackage(String username);
    
    void updateUserProfile(Long userId, String email, PackageType packageType);
    
    Map<String, Object> getUserStatistics(String username);
    
    void applyPendingPackageChanges();
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);

    void deleteUserByAdmin(Long userId);

}
