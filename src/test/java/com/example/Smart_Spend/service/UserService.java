package com.example.Smart_Spend.service;

import com.example.Smart_Spend.entity.User;
import com.example.Smart_Spend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loadUserByUsername_ShouldReturnUser_WhenEmailVerified() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setEmailVerified(true);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        var result = userService.loadUserByUsername("test@example.com");

        assertEquals(user, result);
    }

    @Test
    void loadUserByUsername_ShouldThrow_WhenEmailNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userService.loadUserByUsername("unknown@example.com"));
    }

    @Test
    void loadUserByUsername_ShouldThrow_WhenEmailNotVerified() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setEmailVerified(false);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThrows(RuntimeException.class,
                () -> userService.loadUserByUsername("test@example.com"));
    }

    @Test
    void registerUser_ShouldSaveUserAndSendEmail() {
        String email = "new@example.com";
        String password = "pass123";

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPass");

        User savedUser = new User();
        savedUser.setEmail(email);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.registerUser(email, password, "John", "Doe");

        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendVerificationEmail(any(User.class));
    }

    @Test
    void verifyEmail_ShouldReturnTrue_WhenTokenValid() {
        User user = new User();
        user.setVerificationToken("abc");

        when(userRepository.findByVerificationToken("abc")).thenReturn(Optional.of(user));

        boolean result = userService.verifyEmail("abc");

        assertTrue(result);
        assertNull(user.getVerificationToken());
        assertTrue(user.isEmailVerified());
    }

    @Test
    void initiatePasswordReset_ShouldSendEmail_WhenUserExists() {
        User user = new User();
        user.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        userService.initiatePasswordReset("test@example.com");

        assertNotNull(user.getResetToken());
        assertNotNull(user.getResetTokenExpiry());
        verify(emailService, times(1)).sendPasswordResetEmail(user);
    }

    @Test
    void resetPassword_ShouldReturnTrue_WhenValidToken() {
        User user = new User();
        user.setResetToken("xyz");
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));

        when(userRepository.findByResetToken("xyz")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass")).thenReturn("encodedPass");

        boolean result = userService.resetPassword("xyz", "newPass");

        assertTrue(result);
        assertNull(user.getResetToken());
        assertNull(user.getResetTokenExpiry());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void resetPassword_ShouldReturnFalse_WhenTokenExpired() {
        User user = new User();
        user.setResetToken("expired");
        user.setResetTokenExpiry(LocalDateTime.now().minusHours(1));

        when(userRepository.findByResetToken("expired")).thenReturn(Optional.of(user));

        boolean result = userService.resetPassword("expired", "newPass");

        assertFalse(result);
    }
}
