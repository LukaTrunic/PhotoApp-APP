package hr.algebra.photoapp.service.policy;

import hr.algebra.photoapp.model.User;
import org.springframework.web.multipart.MultipartFile;

/**
 * SRP: upload eligibility rules only (limits, file size, daily reset).
 * DIP: PhotoServiceImpl depends on this abstraction, not validation details.
 */
public interface PhotoUploadPolicy {

    void validateAndPrepareForUpload(User user, MultipartFile file);
}
