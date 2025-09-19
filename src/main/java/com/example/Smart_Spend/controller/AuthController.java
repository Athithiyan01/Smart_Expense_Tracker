package com.example.Smart_Spend.controller;

import com.example.Smart_Spend.entity.User;
import com.example.Smart_Spend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/")
    public String landingPage() {
        return "landing"; // loads templates/landing.html
    }
    
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                          @RequestParam(value = "logout", required = false) String logout,
                          Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password");
        }
        if (logout != null) {
            model.addAttribute("message", "Logged out successfully");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute User user, BindingResult result,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerUser(user.getEmail(), user.getPassword(),
                                   user.getFirstName(), user.getLastName());
            redirectAttributes.addFlashAttribute("message",
                "Registration successful! Check your email for verification link.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            result.rejectValue("email", "error.user", e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token, RedirectAttributes redirectAttributes) {
        if (userService.verifyEmail(token)) {
            redirectAttributes.addFlashAttribute("message", "Email verified successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid verification token");
        }
        return "redirect:/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email, RedirectAttributes redirectAttributes) {
        userService.initiatePasswordReset(email);
        redirectAttributes.addFlashAttribute("message",
            "Password reset link sent to your email if account exists");
        return "redirect:/login";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token, @RequestParam String password,
                              RedirectAttributes redirectAttributes) {
        if (userService.resetPassword(token, password)) {
            redirectAttributes.addFlashAttribute("message", "Password reset successful!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired reset token");
        }
        return "redirect:/login";
    }
}