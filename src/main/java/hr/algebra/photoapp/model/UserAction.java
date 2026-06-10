package hr.algebra.photoapp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// Entity Pattern (Domain Model)
// Represents user actions for logging and auditing
@Entity
@Table(name = "user_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String username; // Store username in case user is deleted

    @Column(nullable = false)
    private String action; // e.g., "LOGIN", "UPLOAD", "DELETE", "EDIT"

    @Column(length = 1000)
    private String details; // Additional details about the action

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private String ipAddress;
}
