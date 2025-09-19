package com.example.Smart_Spend.service;

import com.example.Smart_Spend.entity.User;
import com.example.Smart_Spend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base.url}")
    private String baseUrl;

    // Constructor injection with @Lazy for PasswordEncoder to break circular dependency
    public UserService(UserRepository userRepository,
                       EmailService emailService,
                       @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // ✅ Return your custom User entity directly (since it implements UserDetails)
        // But we need to check if email is verified for login
        if (!user.isEmailVerified()) {
            throw new RuntimeException("Email not verified. Please check your email and verify your account.");
        }
        
        return user; // Your User entity implements UserDetails
    }

    // Add this method to find your custom User entity
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    // Or better yet, return Optional<User>
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User registerUser(String email, String password, String firstName, String lastName) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setRole(User.Role.USER);

        User savedUser = userRepository.save(user);
        emailService.sendVerificationEmail(savedUser);
        return savedUser;
    }

    public boolean verifyEmail(String token) {
        Optional<User> userOpt = userRepository.findByVerificationToken(token);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setEmailVerified(true);
            user.setVerificationToken(null);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public void initiatePasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setResetToken(UUID.randomUUID().toString());
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(24));
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user);
        }
    }

    public boolean resetPassword(String token, String newPassword) {
        Optional<User> userOpt = userRepository.findByResetToken(token);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getResetTokenExpiry().isAfter(LocalDateTime.now())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetToken(null);
                user.setResetTokenExpiry(null);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    public void createAdminUser() {
        if (!userRepository.existsByEmail("admin@smartspend.com")) {
            User admin = new User();
            admin.setEmail("admin@smartspend.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setRole(User.Role.ADMIN);
            admin.setEmailVerified(true);
            userRepository.save(admin);
        }
    }

    public void createDemoUser() {
        if (!userRepository.existsByEmail("demo@smartspend.com")) {
            User demo = new User();
            demo.setEmail("demo@smartspend.com");
            demo.setPassword(passwordEncoder.encode("demo123"));
            demo.setFirstName("Demo");
            demo.setLastName("User");
            demo.setRole(User.Role.USER);
            demo.setEmailVerified(true); // ensure demo user can log in
            userRepository.save(demo);
        }
    }
}