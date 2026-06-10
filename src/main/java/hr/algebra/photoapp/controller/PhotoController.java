package hr.algebra.photoapp.controller;

import hr.algebra.photoapp.metrics.PhotoAppMetrics;
import hr.algebra.photoapp.util.FunctionalPhotoHelpers;
import hr.algebra.photoapp.model.Photo;
import hr.algebra.photoapp.model.User;
import hr.algebra.photoapp.service.PhotoService;
import hr.algebra.photoapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

// Controller Pattern (MVC) + Facade Pattern
// Handles all photo-related HTTP requests
@Controller
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;
    private final UserService userService;
    private final PhotoAppMetrics photoAppMetrics;

    // UPLOAD
    @GetMapping("/photos/upload")
    public String showUploadForm(Model model, Authentication authentication) {

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {

            User user = getLoggedUser(authentication);
            if (user != null) {
                model.addAttribute("user", user);
                model.addAttribute("stats", userService.getUserStatistics(user.getUsername()));
            }
        }

        return "upload";
    }


    @PostMapping("/photos/upload")
    public String upload(@RequestParam MultipartFile file,
                         @RequestParam String description,
                         @RequestParam String hashtags,
                         @RequestParam(required = false) String format,
                         @RequestParam(required = false) Integer width,
                         @RequestParam(required = false) Integer height,
                         RedirectAttributes redirectAttributes,
                         Authentication authentication) {

        User user = getLoggedUser(authentication);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Please login first");
            return "redirect:/login";
        }

        try {
            photoService.upload(file, description, hashtags, format, width, height);
            redirectAttributes.addFlashAttribute("success", "Photo uploaded successfully");
        } catch (IllegalStateException e) {
            if ("UPLOAD_LIMIT".equals(e.getMessage())) {
                redirectAttributes.addFlashAttribute("error", 
                        "Daily upload limit reached for your package");
            } else if ("FILE_TOO_LARGE".equals(e.getMessage())) {
                redirectAttributes.addFlashAttribute("error", 
                        "File is too large for your package");
            } else {
                redirectAttributes.addFlashAttribute("error", 
                        "Error: " + e.getMessage());
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", 
                    "Failed to upload photo: " + e.getMessage());
        }

        return "redirect:/photos";
    }

    // GALLERY & SEARCH
    @GetMapping("/photos")
    public String gallery(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "date") String sort,
                          @RequestParam(required = false) String hashtag,
                          @RequestParam(required = false) String author,
                          @RequestParam(required = false) Long minSize,
                          @RequestParam(required = false) Long maxSize,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                          Model model,
                          Authentication authentication) {

        // Convert dates to LocalDateTime
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        boolean hasFilters =
                hashtag != null || author != null ||
                        minSize != null || maxSize != null ||
                        start != null || end != null;

        if (!hasFilters && page == 0) {
            var latest = photoService.getLatestPhotos(10);
            if (hashtag != null && !hashtag.isBlank()) {
                latest = FunctionalPhotoHelpers.filterByHashtag(latest, hashtag);
            }
            model.addAttribute("photos", latest);
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 1);
            model.addAttribute("sort", "date");
        } else {
            var photoPage = hasFilters
                    ? photoService.searchPhotos(hashtag, author, minSize, maxSize, start, end, page, 12)
                    : photoService.findAllPagedSorted(page, 12, sort);

            model.addAttribute("photos", photoPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", photoPage.getTotalPages());
            model.addAttribute("sort", sort);
        }


        // Keep search parameters
        model.addAttribute("hashtag", hashtag);
        model.addAttribute("author", author);
        model.addAttribute("minSize", minSize);
        model.addAttribute("maxSize", maxSize);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {
            User user = getLoggedUser(authentication);
            if (user != null) {
                model.addAttribute("currentUser", user);
            }
        }

        return "photos";
    }

    // EDIT
    @GetMapping("/photos/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               Model model,
                               Authentication authentication) {

        User currentUser = getLoggedUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        Photo photo = photoService.findById(id);
        if (photo == null) {
            return "redirect:/photos";
        }

        boolean isOwner = photo.getOwner().getUsername()
                .equals(currentUser.getUsername());

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            return "redirect:/photos";
        }

        model.addAttribute("photo", photo);
        return "edit-photo";
    }

    @PostMapping("/photos/edit/{id}")
    public String editPhoto(@PathVariable Long id,
                            @RequestParam String description,
                            @RequestParam String hashtags,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {

        photoService.updatePhoto(id, description, hashtags, authentication);
        redirectAttributes.addFlashAttribute("success", "✅ Photo updated successfully");
        return "redirect:/photos";
    }

    // MY PHOTOS
    @GetMapping("/photos/mine")
    public String myPhotos(Model model, Authentication authentication) {

        User user = getLoggedUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("photos", photoService.findMine());
        model.addAttribute("myPhotos", true);
        model.addAttribute("currentUser", user);
        model.addAttribute("stats", userService.getUserStatistics(user.getUsername()));

        return "my-photos";
    }


    // DELETE
    @PostMapping("/photos/delete/{id}")
    public String deletePhoto(@PathVariable Long id,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {

        photoService.deletePhoto(id, authentication);
        redirectAttributes.addFlashAttribute("success", "✅ Photo deleted successfully");
        return "redirect:/photos";
    }

    // LIKE
    @PostMapping("/photos/like/{id}")
    public String likePhoto(@PathVariable Long id) {
        photoService.likePhoto(id);
        return "redirect:/photos";
    }

    @PostMapping("/photos/like-ajax/{id}")
    @ResponseBody
    public Map<String, Object> likeAjax(@PathVariable Long id) {

        boolean liked = photoService.toggleLike(id);
        int likes = photoService.findById(id).getLikes();

        return Map.of(
                "liked", liked,
                "likes", likes
        );
    }

    // DOWNLOAD
    @GetMapping("/photos/download/{id}")
    public ResponseEntity<byte[]> downloadPhoto(
            @PathVariable Long id,
            @RequestParam(defaultValue = "original") String format,
            @RequestParam(defaultValue = "false") boolean sepia,
            @RequestParam(defaultValue = "false") boolean blur,
            @RequestParam(required = false) Integer width,
            @RequestParam(required = false) Integer height
    ) throws IOException {

        Photo photo = photoService.findById(id);

        byte[] imageBytes;
        String filename;
        String contentType;

        boolean hasFilters = sepia || blur || width != null || height != null;

        if ("original".equalsIgnoreCase(format) && !hasFilters) {
            // original download
            imageBytes = photoService.getPhotoBytes(photo);
            filename = photo.getOriginalFilename();
            contentType = photo.getContentType() != null
                    ? photo.getContentType()
                    : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        } else {
            // Filtered download
            String outputFormat = "original".equalsIgnoreCase(format) ? "jpg" : format;

            if (hasFilters) {
                imageBytes = photoService.getPhotoWithFilters(
                        photo, outputFormat, width, height, sepia, blur
                );
            } else {
                imageBytes = photoService.getPhotoAsFormat(photo, outputFormat);
            }

            filename = removeExtension(photo.getOriginalFilename()) + "." + outputFormat;
            contentType = "image/" + outputFormat;
        }

        photoAppMetrics.recordDownload();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(imageBytes);
    }


    // VIEW FULL IMAGE
    @GetMapping("/photos/view/{id}")
    public ResponseEntity<byte[]> viewPhoto(@PathVariable Long id) throws IOException {
        Photo photo = photoService.findById(id);
        byte[] imageBytes = photoService.getPhotoBytes(photo);

        MediaType mediaType = MediaType.parseMediaType(
                photo.getContentType() != null ? photo.getContentType() : "image/jpeg");

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(imageBytes);
    }

    private User getLoggedUser(Authentication authentication) {
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
            return null;
        }

        return userService.findByUsername(authentication.getName());
    }

    private String removeExtension(String filename) {
        if (filename == null) return "image";
        int dot = filename.lastIndexOf('.');
        return (dot > 0) ? filename.substring(0, dot) : filename;
    }


}
