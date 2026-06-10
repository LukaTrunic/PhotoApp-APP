package hr.algebra.photoapp.service.impl;

import hr.algebra.photoapp.metrics.PhotoAppMetrics;
import hr.algebra.photoapp.model.Photo;
import hr.algebra.photoapp.model.User;
import hr.algebra.photoapp.repository.PhotoRepository;
import hr.algebra.photoapp.repository.UserRepository;
import hr.algebra.photoapp.service.PhotoService;
import hr.algebra.photoapp.service.UserActionService;
import hr.algebra.photoapp.service.policy.PhotoAuthorizationService;
import hr.algebra.photoapp.service.policy.PhotoUploadPolicy;
import hr.algebra.photoapp.service.storage.PhotoStorageStrategy;
import hr.algebra.photoapp.util.FunctionalPhotoHelpers;
import hr.algebra.photoapp.util.ImageProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

// Service Layer Pattern + Strategy Pattern (for storage) + Facade Pattern + Builder Pattern
// Implements photo management business logic with image processing capabilities
@Service
@RequiredArgsConstructor
@Transactional
public class PhotoServiceImpl implements PhotoService {

    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final PhotoStorageStrategy storage;
    private final UserActionService userActionService;
    private final PhotoUploadPolicy uploadPolicy;
    private final PhotoAuthorizationService photoAuthorizationService;
    private final PhotoAppMetrics photoAppMetrics;

    @Override
    public Photo upload(MultipartFile file, String description, String hashtags) throws IOException {
        return upload(file, description, hashtags, null, null, null);
    }

    @Override
    public Photo upload(MultipartFile file, String description, String hashtags, 
                       String format, Integer resizeWidth, Integer resizeHeight) throws IOException {

        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        uploadPolicy.validateAndPrepareForUpload(user, file);

        byte[] fileBytes = file.getBytes();

        String originalFilename = file.getOriginalFilename();
        String filename = originalFilename;

        // Apply image processing if requested
        if (format != null || resizeWidth != null || resizeHeight != null) {
            BufferedImage image = ImageProcessor.loadImage(fileBytes);

            if (resizeWidth != null || resizeHeight != null) {
                int w = resizeWidth != null ? resizeWidth : 0;
                int h = resizeHeight != null ? resizeHeight : 0;
                image = ImageProcessor.resize(image, w, h);
            }

            String outputFormat = format != null ? format : "jpg";
            fileBytes = ImageProcessor.convertFormat(image, outputFormat);

            filename = removeExtension(originalFilename) + "." + outputFormat;
        }


        // Store the file
        try {
            storage.store(username, filename, fileBytes);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store uploaded file");
        }

        // Get image dimensions
        Dimension dimensions = ImageProcessor.getImageDimensions(fileBytes);

        // Create photo
        Photo photo = Photo.builder()
                .filename("/uploads/" + username + "/" + filename) // storage name
                .originalFilename(originalFilename)
                .contentType(file.getContentType())
                .size(fileBytes.length)
                .description(description)
                .hashtags(FunctionalPhotoHelpers.normalizeHashtags(hashtags))
                .uploadedAt(LocalDateTime.now())
                .owner(user)
                .width(dimensions.width)
                .height(dimensions.height)
                .build();

        photoRepository.save(photo);

        // Update user statistics
        user.setUploadsToday(user.getUploadsToday() + 1);
        user.addUploadSize(fileBytes.length);
        userRepository.save(user);

        // Log action
        userActionService.logAction(user, "UPLOAD_PHOTO", 
                String.format("Uploaded photo: %s (%.2f MB)", filename, 
                        fileBytes.length / (1024.0 * 1024.0)), null);

        photoAppMetrics.recordUpload();

        return photo;
    }

    @Override
    @Transactional(readOnly = true)
    public Photo findById(Long id) {
        return photoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Photo not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Photo> findAll() {
        return photoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Photo> findAllPagedSorted(int page, int size, String sort) {
        PageRequest pageRequest;

        if ("likes".equals(sort)) {
            // Custom sorting by likes count (would need query)
            pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        } else {
            pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadedAt"));
        }

        return photoRepository.findAll(pageRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Photo> searchPhotos(String hashtag, String author, Long minSize, Long maxSize,
                                   LocalDateTime startDate, LocalDateTime endDate, 
                                   int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return photoRepository.searchPhotos(hashtag, author, minSize, maxSize, 
                startDate, endDate, pageRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Photo> findMine() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        return photoRepository.findByOwnerOrderByUploadedAtDesc(user);
    }

    @Override
    public void updatePhoto(Long id, String description, String hashtags, Authentication authentication) {
        Photo photo = findById(id);
        photoAuthorizationService.requireCanModify(photo, authentication);

        photo.setDescription(description);
        photo.setHashtags(FunctionalPhotoHelpers.normalizeHashtags(hashtags));
        photoRepository.save(photo);

        // Log action
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user != null) {
            userActionService.logAction(user, "EDIT_PHOTO", 
                    String.format("Edited photo: %s", photo.getOriginalFilename()), null);
        }
    }

    @Override
    public void deletePhoto(Long id, Authentication authentication) {
        Photo photo = findById(id);
        photoAuthorizationService.requireCanModify(photo, authentication);

        // Delete from storage
        try {
            storage.delete(photo.getFilename());
        } catch (Exception e) {

        }

        photoRepository.delete(photo);

        // Log action
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user != null) {
            userActionService.logAction(user, "DELETE_PHOTO", 
                    String.format("Deleted photo: %s", photo.getOriginalFilename()), null);
        }

        photoAppMetrics.recordDeletion();
    }

    @Override
    public void likePhoto(Long id) {
        toggleLike(id);
    }

    @Override
    public boolean toggleLike(Long id) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        Photo photo = findById(id);

        boolean liked;
        if (photo.getLikedBy().contains(user)) {
            photo.getLikedBy().remove(user);
            liked = false;
        } else {
            photo.getLikedBy().add(user);
            liked = true;
        }

        photoRepository.save(photo);

        // Log action
        userActionService.logAction(user, liked ? "LIKE_PHOTO" : "UNLIKE_PHOTO", 
                String.format("Photo: %s", photo.getOriginalFilename()), null);

        return liked;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getPhotoAsFormat(Photo photo, String format) throws IOException {
        byte[] originalBytes = storage.load(photo.getFilename());
        
        if (format == null || format.isEmpty()) {
            return originalBytes;
        }

        BufferedImage image = ImageProcessor.loadImage(originalBytes);
        return ImageProcessor.convertFormat(image, format);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getPhotoWithFilters(Photo photo, String format, Integer width, Integer height,
                                     boolean sepia, boolean blur) throws IOException {
        byte[] originalBytes = storage.load(photo.getFilename());
        BufferedImage image = ImageProcessor.loadImage(originalBytes);

        // Apply filters
        image = ImageProcessor.applyFilters(image, width, height, sepia, blur);

        // Convert to desired format
        String outputFormat = (format != null && !format.isEmpty()) ? format : "jpg";
        return ImageProcessor.convertFormat(image, outputFormat);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getPhotoBytes(Photo photo) throws IOException {
        return storage.load(photo.getFilename());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Photo> getLatestPhotos(int limit) {
        return photoRepository.findTop10ByOrderByUploadedAtDesc();
    }

    private String removeExtension(String filename) {
        if (filename == null) return "image";
        int dot = filename.lastIndexOf('.');
        return (dot > 0) ? filename.substring(0, dot) : filename;
    }
}
