package hr.algebra.photoapp.config;

import hr.algebra.photoapp.model.PackageType;
import hr.algebra.photoapp.model.Role;
import hr.algebra.photoapp.model.User;
import hr.algebra.photoapp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

// Initializer Pattern
// Initializes default data on application startup
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initUsers(UserRepository userRepository,
                                PasswordEncoder passwordEncoder) {
        return args -> {

            if (userRepository.findByUsername("admin").isEmpty()) {

                User admin = User.builder()
                        .username("admin")
                        .email("admin@photoapp.com")
                        .password(passwordEncoder.encode("admin123"))
                        .role(Role.ADMIN)
                        .packageType(PackageType.GOLD)
                        .uploadsToday(0)
                        .totalUploadsSize(0L)
                        .registeredAt(LocalDateTime.now())
                        .build();

                userRepository.save(admin);

                System.out.println("ADMIN USER CREATED");
                System.out.println("Username: admin");
                System.out.println("Password: admin123");
                System.out.println("Package: GOLD (unlimited)");
            } else {
                System.out.println("Admin user already exists");
            }
        };
    }
}
