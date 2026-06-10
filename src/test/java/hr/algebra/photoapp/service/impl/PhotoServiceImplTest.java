package hr.algebra.photoapp.service.impl;

import hr.algebra.photoapp.model.PackageType;
import hr.algebra.photoapp.model.Photo;
import hr.algebra.photoapp.model.User;
import hr.algebra.photoapp.repository.PhotoRepository;
import hr.algebra.photoapp.repository.UserRepository;
import hr.algebra.photoapp.service.UserActionService;
import hr.algebra.photoapp.service.policy.PhotoAuthorizationService;
import hr.algebra.photoapp.service.policy.PhotoUploadPolicy;
import hr.algebra.photoapp.metrics.PhotoAppMetrics;
import hr.algebra.photoapp.service.storage.PhotoStorageStrategy;
import hr.algebra.photoapp.support.SecurityTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoServiceImplTest {

    @Mock private PhotoRepository photoRepository;
    @Mock private UserRepository userRepository;
    @Mock private PhotoStorageStrategy storage;
    @Mock private UserActionService userActionService;
    @Mock private PhotoUploadPolicy uploadPolicy;
    @Mock private PhotoAuthorizationService photoAuthorizationService;
    @Mock private PhotoAppMetrics photoAppMetrics;

    @InjectMocks
    private PhotoServiceImpl photoService;

    private User freeUser;

    @BeforeEach
    void setUp() {
        freeUser = User.builder()
                .id(1L)
                .username("testuser")
                .packageType(PackageType.FREE)
                .uploadsToday(0)
                .lastUploadDate(LocalDate.now())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityTestHelper.clearLogin();
    }

    @Test
    void upload_throwsWhenDailyLimitReached() throws IOException {
        SecurityTestHelper.loginAs("testuser", "USER");
        freeUser.setUploadsToday(PackageType.FREE.getMaxUploadsPerDay());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(freeUser));

        MultipartFile file = mock(MultipartFile.class);
        doThrow(new IllegalStateException("UPLOAD_LIMIT"))
                .when(uploadPolicy).validateAndPrepareForUpload(freeUser, file);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> photoService.upload(file, "desc", "#tag"));

        assertEquals("UPLOAD_LIMIT", ex.getMessage());
        verify(storage, never()).store(anyString(), anyString(), any());
    }

    @Test
    void updatePhoto_forbiddenForOtherUser() {
        User owner = User.builder().id(1L).username("owner").build();
        Photo photo = Photo.builder().id(5L).owner(owner).build();

        when(photoRepository.findById(5L)).thenReturn(Optional.of(photo));

        UserDetails auth = new org.springframework.security.core.userdetails.User(
                "intruder", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                auth, auth.getPassword(), auth.getAuthorities());
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not authorized"))
                .when(photoAuthorizationService).requireCanModify(photo, authentication);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> photoService.updatePhoto(5L, "hack", "#hack", authentication));

        assertEquals(403, ex.getStatusCode().value());
        verify(photoRepository, never()).save(any());
    }
}
