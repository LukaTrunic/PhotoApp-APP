package hr.algebra.photoapp.controller;

import hr.algebra.photoapp.service.security.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class FirebaseAuthController {

    private final CustomUserDetailsService userDetailsService;

    public FirebaseAuthController(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/firebase-login")
    @ResponseBody
    public ResponseEntity<?> firebaseLogin(@RequestParam String idToken,
                                          HttpServletRequest request) {
        try {
            System.out.println("Firebase login attempt...");
            
            Authentication auth = userDetailsService.authenticateFirebaseUser(idToken);

            if (auth == null || !auth.isAuthenticated()) {
                System.err.println("Authentication failed - auth is null or not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication failed"));
            }

            SecurityContextHolder.getContext().setAuthentication(auth);
            request.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext()
            );

            System.out.println("Firebase login successful for: " + auth.getName());
            return ResponseEntity.ok().body(Map.of("success", true));

        } catch (Exception e) {
            System.err.println("Firebase login error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
