package hr.algebra.photoapp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// Entity Pattern (Domain Model)
// Represents a photo with metadata and relationships
@Entity
@Table(name = "photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;          // stored path
    private String originalFilename;  // uploaded name
    private String contentType;
    private long size;

    @Column(length = 2000)
    private String description;

    @Column(length = 500)
    private String hashtags;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToMany
    @JoinTable(
            name = "photo_likes",
            joinColumns = @JoinColumn(name = "photo_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> likedBy = new HashSet<>();
    
    // Image dimensions
    private Integer width;
    private Integer height;

    // Helper methods
    public int getLikes() {
        return likedBy != null ? likedBy.size() : 0;
    }
    
    public String getAuthorName() {
        return owner != null ? owner.getUsername() : "Anonymous";
    }
    
    public String getSizeInMB() {
        return String.format("%.2f MB", size / (1024.0 * 1024.0));
    }
}
