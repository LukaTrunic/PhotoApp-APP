package hr.algebra.photoapp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

// Entity Pattern (Domain Model)
// Tracks package change requests and their effective dates
@Entity
@Table(name = "package_changes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackageType oldPackage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackageType newPackage;

    @Column(nullable = false)
    private LocalDate requestDate;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    @Column(nullable = false)
    private boolean applied = false;
}
