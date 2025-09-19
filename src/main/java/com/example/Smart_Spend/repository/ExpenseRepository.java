package com.example.Smart_Spend.repository;

import com.example.Smart_Spend.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserIdOrderByTransactionDateDesc(Long userId);

    List<Expense> findByUserIdAndCategoryOrderByTransactionDateDesc(Long userId, String category);

    @Query("SELECT e FROM Expense e WHERE e.user.id = :userId AND e.transactionDate BETWEEN :startDate AND :endDate ORDER BY e.transactionDate DESC")
    List<Expense> findByUserAndDateRange(@Param("userId") Long userId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user.id = :userId AND e.type = 'EXPENSE' AND MONTH(e.transactionDate) = :month AND YEAR(e.transactionDate) = :year")
    BigDecimal getTotalExpensesByUserAndMonth(@Param("userId") Long userId,
                                              @Param("month") int month,
                                              @Param("year") int year);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user.id = :userId AND e.type = 'EXPENSE' AND e.category = :category AND MONTH(e.transactionDate) = :month AND YEAR(e.transactionDate) = :year")
    BigDecimal getTotalExpensesByCategoryAndMonth(@Param("userId") Long userId,
                                                  @Param("category") String category,
                                                  @Param("month") int month,
                                                  @Param("year") int year);

    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.user.id = :userId AND e.type = 'EXPENSE' AND MONTH(e.transactionDate) = :month AND YEAR(e.transactionDate) = :year GROUP BY e.category")
    List<Object[]> getCategoryWiseExpenses(@Param("userId") Long userId,
                                           @Param("month") int month,
                                           @Param("year") int year);
}