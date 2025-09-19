package com.example.Smart_Spend.controller;

import com.example.Smart_Spend.entity.User;
import com.example.Smart_Spend.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportsController {

    private final ExpenseService expenseService;

    @GetMapping
    public String showReports(@AuthenticationPrincipal User user, Model model,
                             @RequestParam(defaultValue = "0") int month,
                             @RequestParam(defaultValue = "0") int year) {
        if (user == null) {
            log.error("User is null in showReports - authentication failed");
            return "redirect:/login";
        }

        // Use current month/year if not specified
        if (month == 0 || year == 0) {
            LocalDate now = LocalDate.now();
            month = month == 0 ? now.getMonthValue() : month;
            year = year == 0 ? now.getYear() : year;
        }

        log.debug("Loading reports for user: {} for {}/{}", user.getEmail(), month, year);

        try {
            // Get category-wise expenses
            List<Object[]> categoryExpenses = expenseService.getCategoryWiseExpenses(user.getId(), month, year);
            
            model.addAttribute("categoryExpenses", categoryExpenses);
            model.addAttribute("selectedMonth", month);
            model.addAttribute("selectedYear", year);
            model.addAttribute("title", "Expense Reports");
            model.addAttribute("user", user);
            
            return "reports/index";
        } catch (Exception e) {
            log.error("Error loading reports: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load reports");
            return "reports/index";
        }
    }

    @GetMapping("/monthly")
    public String monthlyReport(@AuthenticationPrincipal User user, Model model,
                               @RequestParam int month, @RequestParam int year) {
        if (user == null) {
            return "redirect:/login";
        }

        List<Object[]> categoryExpenses = expenseService.getCategoryWiseExpenses(user.getId(), month, year);
        
        model.addAttribute("categoryExpenses", categoryExpenses);
        model.addAttribute("month", month);
        model.addAttribute("year", year);
        model.addAttribute("title", String.format("Monthly Report - %d/%d", month, year));
        
        return "reports/monthly";
    }
}