package hr.algebra.photoapp.service.policy;

import hr.algebra.photoapp.model.PackageType;
import hr.algebra.photoapp.model.User;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Component
public class DefaultPhotoUploadPolicy implements PhotoUploadPolicy {

    @Override
    public void validateAndPrepareForUpload(User user, MultipartFile file) {
        resetDailyCounterIfNeeded(user);

        PackageType pkg = user.getPackageType();

        if (user.getUploadsToday() >= pkg.getMaxUploadsPerDay()) {
            throw new IllegalStateException("UPLOAD_LIMIT");
        }

        if (file.getSize() > pkg.getMaxFileSize()) {
            throw new IllegalStateException("FILE_TOO_LARGE");
        }
    }

    private void resetDailyCounterIfNeeded(User user) {
        LocalDate today = LocalDate.now();
        if (user.getLastUploadDate() == null || !user.getLastUploadDate().equals(today)) {
            user.setUploadsToday(0);
            user.setLastUploadDate(today);
        }
    }
}
