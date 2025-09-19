package com.example.Smart_Spend.controller;

import com.example.Smart_Spend.entity.User;
import com.example.Smart_Spend.entity.Budget;
import com.example.Smart_Spend.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public String listBudgets(@AuthenticationPrincipal User user, Model model) {
        List<Budget> budgets = budgetService.getBudgetsByUser(user.getId());
        model.addAttribute("budgets", budgets);
        model.addAttribute("title", "Budget Management");
        return "budgets/list";
    }

    @GetMapping("/new")
    public String newBudgetForm(Model model) {
        Budget budget = new Budget();
        YearMonth currentMonth = YearMonth.now();
        budget.setMonth(currentMonth.getMonthValue());
        budget.setYear(currentMonth.getYear());
        model.addAttribute("budget", budget);
        model.addAttribute("title", "Create New Budget");
        return "budgets/form";
    }

    @PostMapping("/new")
    public String createBudget(@Valid @ModelAttribute Budget budget, BindingResult result,
                             @AuthenticationPrincipal User user, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "budgets/form";
        }

        budget.setUser(user);
        budgetService.saveBudget(budget);
        redirectAttributes.addFlashAttribute("message", "Budget created successfully!");
        return "redirect:/budgets";
    }

    @GetMapping("/{id}/edit")
    public String editBudgetForm(@PathVariable Long id, @AuthenticationPrincipal User user, Model model) {
        Optional<Budget> budget = budgetService.findById(id);
        if (budget.isPresent() && budget.get().getUser().getId().equals(user.getId())) {
            model.addAttribute("budget", budget.get());
            model.addAttribute("title", "Edit Budget");
            return "budgets/form";
        }
        return "redirect:/budgets";
    }

    @PostMapping("/{id}/edit")
    public String updateBudget(@PathVariable Long id, @Valid @ModelAttribute Budget budget,
                             BindingResult result, @AuthenticationPrincipal User user,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "budgets/form";
        }

        Optional<Budget> existingBudget = budgetService.findById(id);
        if (existingBudget.isPresent() && existingBudget.get().getUser().getId().equals(user.getId())) {
            budget.setId(id);
            budget.setUser(user);
            budgetService.saveBudget(budget);
            redirectAttributes.addFlashAttribute("message", "Budget updated successfully!");
        }
        return "redirect:/budgets";
    }

    @PostMapping("/{id}/delete")
    public String deleteBudget(@PathVariable Long id, @AuthenticationPrincipal User user,
                             RedirectAttributes redirectAttributes) {
        Optional<Budget> budget = budgetService.findById(id);
        if (budget.isPresent() && budget.get().getUser().getId().equals(user.getId())) {
            budgetService.deleteBudget(id);
            redirectAttributes.addFlashAttribute("message", "Budget deleted successfully!");
        }
        return "redirect:/budgets";
    }
}