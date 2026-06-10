package hr.algebra.photoapp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// Entity Pattern (Domain Model)
// Represents a user in the system with authentication and package details
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_username", columnList = "username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 255)
    private String username;

    private String displayName;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackageType packageType;

    @Column(nullable = false)
    private int uploadsToday = 0;
    
    @Column(nullable = false)
    private long totalUploadsSize = 0L; // Total size of all uploads in bytes

    private LocalDate lastUploadDate;
    
    private LocalDateTime registeredAt;
    
    private LocalDate lastPackageChangeDate; // Track when user last changed package

    @ManyToMany(mappedBy = "likedBy")
    private Set<Photo> likedPhotos = new HashSet<>();
    
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private Set<Photo> photos = new HashSet<>();

    // Helper methods
    public void incrementUploadsToday() {
        this.uploadsToday++;
    }

    public void resetUploadsToday() {
        this.uploadsToday = 0;
    }

    public void addUploadSize(long size) {
        this.totalUploadsSize += size;
    }

    public boolean canChangePackageToday() {
        if (lastPackageChangeDate == null) {
            return true;
        }
        return !lastPackageChangeDate.equals(LocalDate.now());
    }
}
