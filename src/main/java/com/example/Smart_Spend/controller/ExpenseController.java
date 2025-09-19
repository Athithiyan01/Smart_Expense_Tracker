package com.example.Smart_Spend.controller;

import com.example.Smart_Spend.entity.User;
import com.example.Smart_Spend.entity.Expense;
import com.example.Smart_Spend.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/expenses")
@RequiredArgsConstructor
@Slf4j
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public String listExpenses(@AuthenticationPrincipal User user, Model model) {
        if (user == null) {
            log.error("User is null in listExpenses - authentication failed");
            return "redirect:/login";
        }
        
        log.debug("Loading expenses for user: {} (ID: {})", user.getEmail(), user.getId());
        
        List<Expense> expenses = expenseService.getExpensesByUser(user.getId());
        model.addAttribute("expenses", expenses);
        model.addAttribute("title", "Expenses");
        model.addAttribute("user", user);
        return "expenses/list";
    }

    // Map both /new and /add to handle different URL patterns
    @GetMapping({"/new", "/add"})
    public String newExpenseForm(Model model) {
        model.addAttribute("expense", new Expense());
        model.addAttribute("title", "Add New Expense");
        return "expenses/form";
    }

    // Handle both POST endpoints
    @PostMapping({"/new", "/add"})
    public String createExpense(@Valid @ModelAttribute Expense expense, 
                              BindingResult result,
                              @AuthenticationPrincipal User user, 
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "expenses/form";
        }
        
        if (user == null) {
            log.error("User is null in createExpense - authentication failed");
            return "redirect:/login";
        }

        expense.setUser(user);
        expenseService.saveExpense(expense);
        redirectAttributes.addFlashAttribute("message", "Expense added successfully!");
        return "redirect:/expenses";
    }

    @GetMapping("/{id}/edit")
    public String editExpenseForm(@PathVariable Long id, 
                                @AuthenticationPrincipal User user, 
                                Model model) {
        if (user == null) {
            log.error("User is null in editExpenseForm - authentication failed");
            return "redirect:/login";
        }
        
        Optional<Expense> expense = expenseService.findById(id);
        if (expense.isPresent() && expense.get().getUser().getId().equals(user.getId())) {
            model.addAttribute("expense", expense.get());
            model.addAttribute("title", "Edit Expense");
            return "expenses/form";
        }
        
        log.warn("User {} attempted to edit expense {} that doesn't belong to them", 
                user.getEmail(), id);
        return "redirect:/expenses";
    }

    @PostMapping("/{id}/edit")
    public String updateExpense(@PathVariable Long id, 
                              @Valid @ModelAttribute Expense expense,
                              BindingResult result, 
                              @AuthenticationPrincipal User user,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "expenses/form";
        }
        
        if (user == null) {
            log.error("User is null in updateExpense - authentication failed");
            return "redirect:/login";
        }

        Optional<Expense> existingExpense = expenseService.findById(id);
        if (existingExpense.isPresent() && existingExpense.get().getUser().getId().equals(user.getId())) {
            expense.setId(id);
            expense.setUser(user);
            expenseService.saveExpense(expense);
            redirectAttributes.addFlashAttribute("message", "Expense updated successfully!");
        } else {
            log.warn("User {} attempted to update expense {} that doesn't belong to them", 
                    user.getEmail(), id);
        }
        return "redirect:/expenses";
    }

    @PostMapping("/{id}/delete")
    public String deleteExpense(@PathVariable Long id, 
                              @AuthenticationPrincipal User user,
                              RedirectAttributes redirectAttributes) {
        if (user == null) {
            log.error("User is null in deleteExpense - authentication failed");
            return "redirect:/login";
        }
        
        Optional<Expense> expense = expenseService.findById(id);
        if (expense.isPresent() && expense.get().getUser().getId().equals(user.getId())) {
            expenseService.deleteExpense(id);
            redirectAttributes.addFlashAttribute("message", "Expense deleted successfully!");
        } else {
            log.warn("User {} attempted to delete expense {} that doesn't belong to them", 
                    user.getEmail(), id);
        }
        return "redirect:/expenses";
    }
}