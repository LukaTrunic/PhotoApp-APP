package hr.algebra.photoapp.service.impl;

import hr.algebra.photoapp.metrics.PhotoAppMetrics;
import hr.algebra.photoapp.model.PackageType;
import hr.algebra.photoapp.model.Role;
import hr.algebra.photoapp.model.User;
import hr.algebra.photoapp.repository.PackageChangeRepository;
import hr.algebra.photoapp.repository.PhotoRepository;
import hr.algebra.photoapp.repository.UserRepository;
import hr.algebra.photoapp.service.UserActionService;
import hr.algebra.photoapp.support.SecurityTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PhotoRepository photoRepository;
    @Mock private PackageChangeRepository packageChangeRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserActionService userActionService;
    @Mock private PhotoAppMetrics photoAppMetrics;

    @InjectMocks
    private UserServiceImpl userService;

    @AfterEach
    void tearDown() {
        SecurityTestHelper.clearLogin();
    }

    @Test
    void registerUser_savesNewUserWithEncodedPassword() {
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "encoded-" + inv.getArgument(0));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(1L);
            return user;
        });

        User saved = userService.registerUser("alice", "alice@test.com", "secret1", PackageType.FREE);

        assertEquals("alice", saved.getUsername());
        assertEquals(Role.USER, saved.getRole());
        assertEquals(PackageType.FREE, saved.getPackageType());
        verify(passwordEncoder).encode("secret1");
        verify(userActionService).logAction(eq(saved), eq("REGISTER"), anyString(), isNull());
        verify(photoAppMetrics).recordRegistration();
    }

    @Test
    void registerUser_throwsWhenUsernameAlreadyExists() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(new User()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser("alice", "alice@test.com", "secret1", PackageType.FREE));

        assertEquals("Username already exists", ex.getMessage());
        verify(userRepository, never()).save(any());
    }
}
