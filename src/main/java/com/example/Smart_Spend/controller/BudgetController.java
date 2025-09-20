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
import java.math.BigDecimal;
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
        try {
            List<Budget> budgets = budgetService.getBudgetsByUser(user.getId());
            model.addAttribute("budgets", budgets);
            model.addAttribute("title", "Budget Management");
            return "budgets/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading budgets: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/new")
    public String newBudgetForm(Model model) {
        try {
            Budget budget = new Budget();
            YearMonth currentMonth = YearMonth.now();
            
            // Set default values to avoid null issues
            budget.setCategory(""); 
            budget.setAmount(BigDecimal.ZERO);
            budget.setMonth(currentMonth.getMonthValue());
            budget.setYear(currentMonth.getYear());
            
            model.addAttribute("budget", budget);
            model.addAttribute("title", "Create New Budget");
            return "budgets/form";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading budget form: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/new")
    public String createBudget(@Valid @ModelAttribute Budget budget, 
                              BindingResult result,
                              @AuthenticationPrincipal User user, 
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "budgets/form";
        }

        try {
            // Ensure required fields are set
            if (budget.getAmount() == null) {
                budget.setAmount(BigDecimal.ZERO);
            }
            if (budget.getMonth() == null) {
                budget.setMonth(YearMonth.now().getMonthValue());
            }
            if (budget.getYear() == null) {
                budget.setYear(YearMonth.now().getYear());
            }
            
            budget.setUser(user);
            budgetService.saveBudget(budget);
            redirectAttributes.addFlashAttribute("message", "Budget created successfully!");
            return "redirect:/budgets";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating budget: " + e.getMessage());
            return "budgets/form";
        }
    }

    @GetMapping("/add")
    public String addBudgetForm(Model model) {
        try {
            Budget budget = new Budget();
            YearMonth currentMonth = YearMonth.now();
            
            // Set default values
            budget.setCategory(""); 
            budget.setAmount(BigDecimal.ZERO);
            budget.setMonth(currentMonth.getMonthValue());
            budget.setYear(currentMonth.getYear());
            
            model.addAttribute("budget", budget);
            model.addAttribute("title", "Create New Budget");
            return "budgets/add";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading budget form: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/add")
    public String createBudgetFromAdd(@Valid @ModelAttribute Budget budget, 
                                     BindingResult result,
                                     @AuthenticationPrincipal User user, 
                                     RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "budgets/add";
        }
        
        try {
            // Ensure required fields are set
            if (budget.getAmount() == null) {
                budget.setAmount(BigDecimal.ZERO);
            }
            if (budget.getMonth() == null) {
                budget.setMonth(YearMonth.now().getMonthValue());
            }
            if (budget.getYear() == null) {
                budget.setYear(YearMonth.now().getYear());
            }
            
            budget.setUser(user);
            budgetService.saveBudget(budget);
            redirectAttributes.addFlashAttribute("message", "Budget created successfully!");
            return "redirect:/budgets";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating budget: " + e.getMessage());
            return "budgets/add";
        }
    }

    @GetMapping("/{id}/edit")
    public String editBudgetForm(@PathVariable Long id, 
                                @AuthenticationPrincipal User user, 
                                Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            Optional<Budget> budgetOpt = budgetService.findById(id);
            if (budgetOpt.isPresent() && budgetOpt.get().getUser().getId().equals(user.getId())) {
                Budget budget = budgetOpt.get();
                
                // Ensure no null values for form binding
                if (budget.getAmount() == null) {
                    budget.setAmount(BigDecimal.ZERO);
                }
                if (budget.getMonth() == null) {
                    budget.setMonth(YearMonth.now().getMonthValue());
                }
                if (budget.getYear() == null) {
                    budget.setYear(YearMonth.now().getYear());
                }
                
                model.addAttribute("budget", budget);
                model.addAttribute("title", "Edit Budget");
                return "budgets/form";
            } else {
                redirectAttributes.addFlashAttribute("error", "Budget not found or access denied");
                return "redirect:/budgets";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error loading budget: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateBudget(@PathVariable Long id, 
                              @Valid @ModelAttribute Budget budget,
                              BindingResult result, 
                              @AuthenticationPrincipal User user,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "budgets/form";
        }

        try {
            Optional<Budget> existingBudgetOpt = budgetService.findById(id);
            if (existingBudgetOpt.isPresent() && existingBudgetOpt.get().getUser().getId().equals(user.getId())) {
                
                // Ensure required fields are set
                if (budget.getAmount() == null) {
                    budget.setAmount(BigDecimal.ZERO);
                }
                if (budget.getMonth() == null) {
                    budget.setMonth(YearMonth.now().getMonthValue());
                }
                if (budget.getYear() == null) {
                    budget.setYear(YearMonth.now().getYear());
                }
                
                budget.setId(id);
                budget.setUser(user);
                budgetService.saveBudget(budget);
                redirectAttributes.addFlashAttribute("message", "Budget updated successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Budget not found or access denied");
            }
            return "redirect:/budgets";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating budget: " + e.getMessage());
            return "redirect:/budgets";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteBudget(@PathVariable Long id, 
                              @AuthenticationPrincipal User user,
                              RedirectAttributes redirectAttributes) {
        try {
            Optional<Budget> budgetOpt = budgetService.findById(id);
            if (budgetOpt.isPresent() && budgetOpt.get().getUser().getId().equals(user.getId())) {
                budgetService.deleteBudget(id);
                redirectAttributes.addFlashAttribute("message", "Budget deleted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Budget not found or access denied");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting budget: " + e.getMessage());
        }
        return "redirect:/budgets";
    }
}