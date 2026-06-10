package hr.algebra.photoapp.service.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import hr.algebra.photoapp.model.PackageType;
import hr.algebra.photoapp.model.Role;
import hr.algebra.photoapp.model.User;
import hr.algebra.photoapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// Builder Pattern + Signleton pattern (Firebase singleton)
// Implements OAuth/Firebase user creation
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Login
    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        return new CustomUserDetails(user);
    }

    // Firebase login
    @Transactional
    public Authentication authenticateFirebaseUser(String idToken) {

        try {
            FirebaseToken decodedToken =
                    FirebaseAuth.getInstance().verifyIdToken(idToken);

            System.out.println("Firebase UID: " + decodedToken.getUid());
            System.out.println("Email: " + decodedToken.getEmail());

            String email = decodedToken.getEmail();
            
            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Email is required from OAuth provider");
            }

            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        // Generate unique username from email
                        String emailLocalPart = email.substring(0, email.indexOf("@")).toLowerCase();

                        String username = emailLocalPart;
                        int counter = 1;

                        while (userRepository.findByUsername(username).isPresent()) {
                            username = emailLocalPart + counter;
                            counter++;
                        }


                        String displayName = decodedToken.getName();

                        if (displayName == null || displayName.isBlank()) {
                            displayName = generateDisplayName(emailLocalPart);
                        }


                        // Builder pattern to ensure all fields are initialized
                        User newUser = User.builder()
                                .username(username)
                                .email(email)
                                .displayName(displayName)
                                .password("")
                                .role(Role.USER)
                                .packageType(PackageType.FREE)
                                .uploadsToday(0)
                                .totalUploadsSize(0L)
                                .registeredAt(LocalDateTime.now())
                                .build();

                        User savedUser = userRepository.save(newUser);
                        System.out.println("New OAuth user created: " + savedUser.getUsername() + " (email: " + savedUser.getEmail() + ")");
                        return savedUser;
                    });

            System.out.println("User authenticated: " + user.getUsername());

            // IMPORTANT: Return CustomUserDetails, not the User entity
            CustomUserDetails userDetails = new CustomUserDetails(user);

            return new UsernamePasswordAuthenticationToken(
                    userDetails, // Use CustomUserDetails as principal
                    null,
                    userDetails.getAuthorities()
            );

        } catch (Exception e) {
            System.err.println("Firebase authentication failed: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid Firebase authentication token: " + e.getMessage()
            );
        }
    }

    private String generateDisplayName(String emailLocalPart) {
        String normalized = emailLocalPart
                .replace(".", " ")
                .replace("_", " ")
                .replace("-", " ");

        String[] parts = normalized.split("\\s+");

        StringBuilder displayName = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                displayName.append(
                        part.substring(0, 1).toUpperCase()
                                + part.substring(1).toLowerCase()
                ).append(" ");
            }
        }

        return displayName.toString().trim();
    }

}
