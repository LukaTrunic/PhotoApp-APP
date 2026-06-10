package hr.algebra.photoapp.service.policy;

import hr.algebra.photoapp.model.PackageType;
import hr.algebra.photoapp.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultPhotoUploadPolicyTest {

    private DefaultPhotoUploadPolicy policy;
    private User user;

    @BeforeEach
    void setUp() {
        policy = new DefaultPhotoUploadPolicy();
        user = User.builder()
                .packageType(PackageType.FREE)
                .uploadsToday(0)
                .lastUploadDate(LocalDate.now())
                .build();
    }

    @Test
    void rejectsUploadWhenDailyLimitReached() {
        user.setUploadsToday(PackageType.FREE.getMaxUploadsPerDay());
        MultipartFile file = mock(MultipartFile.class);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> policy.validateAndPrepareForUpload(user, file));

        assertEquals("UPLOAD_LIMIT", ex.getMessage());
    }

    @Test
    void rejectsUploadWhenFileTooLarge() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(PackageType.FREE.getMaxFileSize() + 1);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> policy.validateAndPrepareForUpload(user, file));

        assertEquals("FILE_TOO_LARGE", ex.getMessage());
    }
}
