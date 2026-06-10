package hr.algebra.photoapp.service;

import hr.algebra.photoapp.model.Photo;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Service Layer Pattern
// Business logic for photo management
public interface PhotoService {

    Photo upload(MultipartFile file, String description, String hashtags) throws IOException;
    
    Photo upload(MultipartFile file, String description, String hashtags, String format, Integer resizeWidth, Integer resizeHeight) throws IOException;

    Photo findById(Long id);

    List<Photo> findAll();

    Page<Photo> findAllPagedSorted(int page, int size, String sort);
    
    Page<Photo> searchPhotos(String hashtag, String author, Long minSize, Long maxSize, 
                             LocalDateTime startDate, LocalDateTime endDate, int page, int size);

    List<Photo> findMine();

    void updatePhoto(Long id, String description, String hashtags, Authentication authentication);

    void deletePhoto(Long id, Authentication authentication);

    void likePhoto(Long id);

    boolean toggleLike(Long id);

    byte[] getPhotoAsFormat(Photo photo, String format) throws IOException;
    
    byte[] getPhotoWithFilters(Photo photo, String format, Integer width, Integer height, 
                               boolean sepia, boolean blur) throws IOException;
    
    byte[] getPhotoBytes(Photo photo) throws IOException;
    
    List<Photo> getLatestPhotos(int limit);
}
