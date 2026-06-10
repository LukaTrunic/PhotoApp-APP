package hr.algebra.photoapp.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

// Configuration Pattern
// Configures Firebase for OAuth authentication (Google, GitHub)
@Configuration
@Slf4j
public class FirebaseConfig {

    @PostConstruct
    public void init() {
        try {
            InputStream serviceAccount =
                    getClass()
                            .getClassLoader()
                            .getResourceAsStream("firebase-service-account.json");

            if (serviceAccount == null) {
                log.warn("Firebase service account file not found. OAuth login (Google/GitHub) will not work.");
                log.warn("You can still use local username/password login.");
                log.warn("To enable OAuth: Add firebase-service-account.json to src/main/resources/");
                return; // Continue without Firebase
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully - OAuth login enabled");
            }

        } catch (Exception e) {
            log.warn("Failed to initialize Firebase: " + e.getMessage());
            log.warn("OAuth login (Google/GitHub) will not work.");
            log.warn("You can still use local username/password login.");
            // Don't throw exception - allow app to continue
        }
    }
}
