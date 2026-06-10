package hr.algebra.photoapp.repository;

import hr.algebra.photoapp.model.Photo;
import hr.algebra.photoapp.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// Repository Pattern + Proxy Pattern
// Abstracts data access for Photo entities with advanced search capabilities
@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findByOwnerOrderByUploadedAtDesc(User owner);

    
    // Advanced search queries
    @Query("SELECT p FROM Photo p WHERE " +
           "(:hashtag IS NULL OR LOWER(p.hashtags) LIKE LOWER(CONCAT('%', :hashtag, '%'))) AND " +
           "(:author IS NULL OR LOWER(p.owner.username) LIKE LOWER(CONCAT('%', :author, '%'))) AND " +
           "(:minSize IS NULL OR p.size >= :minSize) AND " +
           "(:maxSize IS NULL OR p.size <= :maxSize) AND " +
           "(:startDate IS NULL OR p.uploadedAt >= :startDate) AND " +
           "(:endDate IS NULL OR p.uploadedAt <= :endDate) " +
           "ORDER BY p.uploadedAt DESC")
    Page<Photo> searchPhotos(
            @Param("hashtag") String hashtag,
            @Param("author") String author,
            @Param("minSize") Long minSize,
            @Param("maxSize") Long maxSize,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
    
    List<Photo> findTop10ByOrderByUploadedAtDesc();
    
    long countByOwner(User owner);
    
    @Query("SELECT SUM(p.size) FROM Photo p WHERE p.owner = :owner")
    Long sumSizeByOwner(@Param("owner") User owner);

    void deleteAllByOwner(User owner);

    @Modifying
    @Query(value = "DELETE FROM photo_likes WHERE user_id = :userId", nativeQuery = true)
    void deleteLikesByUserId(@Param("userId") Long userId);

}
