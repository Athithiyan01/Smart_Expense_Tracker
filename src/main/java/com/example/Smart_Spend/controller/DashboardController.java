package com.example.Smart_Spend.controller;

import com.example.Smart_Spend.entity.User;
import com.example.Smart_Spend.entity.Expense;
import com.example.Smart_Spend.entity.Budget;
import com.example.Smart_Spend.service.ExpenseService;
import com.example.Smart_Spend.service.BudgetService;
import com.example.Smart_Spend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.YearMonth;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final ExpenseService expenseService;
    private final BudgetService budgetService;
    private final UserService userService; // Add this injection

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        // Get the authenticated user's email
        String email = authentication.getName();
        
        // Find the user by email from your database
        User user = userService.findByEmail(email);
        
        // Add null check for safety
        if (user == null) {
            return "redirect:/login?error=user_not_found";
        }
        
        model.addAttribute("username", user.getFirstName());

        List<Expense> expenses = expenseService.getExpensesByUser(user.getId());
        List<Budget> budgets = budgetService.getBudgetsByUser(user.getId());

        YearMonth currentMonth = YearMonth.now();
        List<Object[]> categoryData = expenseService.getCategoryWiseExpenses(
                user.getId(), currentMonth.getMonthValue(), currentMonth.getYear());

        model.addAttribute("expenses", expenses.subList(0, Math.min(5, expenses.size())));
        model.addAttribute("budgets", budgets);
        model.addAttribute("categoryData", categoryData);
        model.addAttribute("currentMonth", currentMonth.getMonth().name());
        model.addAttribute("title", "Dashboard");

        return "dashboard/index";
    }
}