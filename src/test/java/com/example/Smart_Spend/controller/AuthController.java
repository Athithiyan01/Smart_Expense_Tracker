package com.example.Smart_Spend.controller;

import com.example.Smart_Spend.entity.User;
import com.example.Smart_Spend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@example.com");
        user.setPassword("12345");
        user.setFirstName("John");
        user.setLastName("Doe");
    }

    @Test
    void shouldReturnLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void shouldReturnRegisterPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    void shouldRegisterUserAndRedirect() throws Exception {
        when(userService.registerUser(any(), any(), any(), any())).thenReturn(user);

        mockMvc.perform(post("/register")
                        .flashAttr("user", user))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(userService, Mockito.times(1))
                .registerUser(user.getEmail(), user.getPassword(), user.getFirstName(), user.getLastName());
    }

    @Test
    void shouldVerifyEmailAndRedirect() throws Exception {
        when(userService.verifyEmail("token123")).thenReturn(true);

        mockMvc.perform(get("/verify-email").param("token", "token123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void shouldHandleForgotPassword() throws Exception {
        mockMvc.perform(post("/forgot-password").param("email", "test@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(userService, Mockito.times(1)).initiatePasswordReset("test@example.com");
    }

    @Test
    void shouldResetPasswordAndRedirect() throws Exception {
        when(userService.resetPassword("token123", "newPass")).thenReturn(true);

        mockMvc.perform(post("/reset-password")
                        .param("token", "token123")
                        .param("password", "newPass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
