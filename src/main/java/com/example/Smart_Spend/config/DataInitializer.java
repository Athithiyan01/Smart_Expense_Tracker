package com.example.Smart_Spend.config;

import com.example.Smart_Spend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) throws Exception {
        // This will run after all beans are initialized
        userService.createAdminUser();
        userService.createDemoUser(); 
    }
}