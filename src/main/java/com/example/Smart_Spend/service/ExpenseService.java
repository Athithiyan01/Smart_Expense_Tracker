package com.example.Smart_Spend.service;

import com.example.Smart_Spend.entity.Expense;
import com.example.Smart_Spend.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final BudgetService budgetService;

    @Transactional(readOnly = true)
    public List<Expense> getExpensesByUser(Long userId) {
        log.debug("Getting expenses for user: {}", userId);
        return expenseRepository.findByUserIdOrderByTransactionDateDesc(userId);
    }

    public Expense saveExpense(Expense expense) {
        log.debug("Saving expense: {} for user: {}", expense.getTitle(), expense.getUser().getId());
        Expense savedExpense = expenseRepository.save(expense);

        try {
            budgetService.checkBudgetAlert(expense.getUser().getId(), expense.getCategory(),
                    expense.getTransactionDate().getMonthValue(),
                    expense.getTransactionDate().getYear());
        } catch (Exception e) {
            log.warn("Failed to check budget alert: {}", e.getMessage());
        }

        return savedExpense;
    }

    public void deleteExpense(Long id) {
        log.debug("Deleting expense: {}", id);
        expenseRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Expense> findById(Long id) {
        return expenseRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getCategoryWiseExpenses(Long userId, int month, int year) {
        log.debug("Getting category-wise expenses for user: {} for {}/{}", userId, month, year);
        return expenseRepository.getCategoryWiseExpenses(userId, month, year);
    }
}