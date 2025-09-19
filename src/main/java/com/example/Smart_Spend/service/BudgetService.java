package com.example.Smart_Spend.service;

import com.example.Smart_Spend.entity.Budget;
import com.example.Smart_Spend.repository.BudgetRepository;
import com.example.Smart_Spend.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public List<Budget> getBudgetsByUser(Long userId) {
        log.debug("Getting budgets for user: {}", userId);
        return budgetRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Budget saveBudget(Budget budget) {
        log.debug("Saving budget: {} for user: {}", budget.getCategory(), budget.getUser().getId());

        Optional<Budget> existing = budgetRepository.findByUserIdAndCategoryAndMonthAndYear(
                budget.getUser().getId(), budget.getCategory(), budget.getMonth(), budget.getYear());

        if (existing.isPresent()) {
            Budget existingBudget = existing.get();
            existingBudget.setAmount(budget.getAmount());
            return budgetRepository.save(existingBudget);
        }
        return budgetRepository.save(budget);
    }

    public void deleteBudget(Long id) {
        log.debug("Deleting budget: {}", id);
        budgetRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Budget> findById(Long id) {
        return budgetRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public void checkBudgetAlert(Long userId, String category, int month, int year) {
        try {
            Optional<Budget> budgetOpt = budgetRepository.findByUserIdAndCategoryAndMonthAndYear(userId, category, month, year);
            if (budgetOpt.isPresent()) {
                Budget budget = budgetOpt.get();
                BigDecimal spent = expenseRepository.getTotalExpensesByCategoryAndMonth(userId, category, month, year);
                if (spent != null && spent.compareTo(budget.getAmount().multiply(BigDecimal.valueOf(0.8))) > 0) {
                    log.info("Budget alert: User {} has spent 80% of budget for category {}", userId, category);
                    // Add notification logic here if needed
                }
            }
        } catch (Exception e) {
            log.warn("Error checking budget alert for user {}: {}", userId, e.getMessage());
        }
    }
}