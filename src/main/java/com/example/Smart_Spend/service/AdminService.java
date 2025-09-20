package com.example.Smart_Spend.service;

import com.example.Smart_Spend.entity.User;
import com.example.Smart_Spend.entity.Expense;
import com.example.Smart_Spend.entity.Budget;
import com.example.Smart_Spend.repository.UserRepository;
import com.example.Smart_Spend.repository.ExpenseRepository;
import com.example.Smart_Spend.repository.BudgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        log.debug("Getting dashboard statistics");
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Basic counts
            long totalUsers = userRepository.count();
            long totalExpenses = expenseRepository.count();
            
            // Total amount calculation
            List<Expense> allExpenses = expenseRepository.findAll();
            BigDecimal totalAmount = allExpenses.stream()
                .filter(e -> e.getType() == Expense.TransactionType.EXPENSE)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Active users (users with expenses in last 30 days)
            LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
            Set<Long> activeUserIds = allExpenses.stream()
                .filter(e -> e.getTransactionDate().isAfter(thirtyDaysAgo))
                .map(e -> e.getUser().getId())
                .collect(Collectors.toSet());
            
            // Recent users - Handle null verification status safely
            List<User> recentUsers = userRepository.findAll().stream()
                .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());
            
            // Monthly stats for chart
            Map<String, BigDecimal> monthlyStats = getMonthlyExpenseStats();
            
            // Category stats
            YearMonth currentMonth = YearMonth.now();
            Map<String, BigDecimal> categoryStats = getCategoryStats(currentMonth.getMonthValue(), currentMonth.getYear());
            
            stats.put("totalUsers", totalUsers);
            stats.put("totalExpenses", totalExpenses);
            stats.put("totalAmount", totalAmount);
            stats.put("activeUsers", activeUserIds.size());
            stats.put("recentUsers", recentUsers);
            stats.put("monthlyStats", monthlyStats);
            stats.put("categoryStats", categoryStats);
            
            log.debug("Dashboard stats calculated successfully");
            return stats;
        } catch (Exception e) {
            log.error("Error calculating dashboard stats: {}", e.getMessage(), e);
            // Return empty stats in case of error
            stats.put("totalUsers", 0L);
            stats.put("totalExpenses", 0L);
            stats.put("totalAmount", BigDecimal.ZERO);
            stats.put("activeUsers", 0);
            stats.put("recentUsers", Collections.emptyList());
            stats.put("monthlyStats", Collections.emptyMap());
            stats.put("categoryStats", Collections.emptyMap());
            return stats;
        }
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        log.debug("Getting all users");
        try {
            return userRepository.findAll().stream()
                    .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting all users: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        log.debug("Getting user by id: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void toggleUserStatus(Long id) {
        log.debug("Toggling user status for id: {}", id);
        try {
            User user = getUserById(id);
            
            // Safe null handling for Boolean field
            Boolean currentStatus = user.getEmailVerified();
            if (currentStatus == null) {
                user.setEmailVerified(true); // Set to verified if null
                log.info("Setting null email verification status to true for user: {}", user.getEmail());
            } else {
                user.setEmailVerified(!currentStatus);
                log.info("Toggling email verification from {} to {} for user: {}", 
                    currentStatus, !currentStatus, user.getEmail());
            }
            
            userRepository.save(user);
            log.info("User status toggled successfully for: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Error toggling user status for id {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to toggle user status: " + e.getMessage());
        }
    }

    public void deleteUser(Long id) {
        log.debug("Deleting user with id: {}", id);
        try {
            User user = getUserById(id);
            if (user.getRole() == User.Role.ADMIN) {
                throw new RuntimeException("Cannot delete admin user");
            }
            userRepository.deleteById(id);
            log.info("User deleted: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Error deleting user with id {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete user: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<Expense> getUserExpenses(Long userId) {
        log.debug("Getting expenses for user: {}", userId);
        try {
            return expenseRepository.findByUserIdOrderByTransactionDateDesc(userId);
        } catch (Exception e) {
            log.error("Error getting expenses for user {}: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public List<Budget> getUserBudgets(Long userId) {
        log.debug("Getting budgets for user: {}", userId);
        try {
            return budgetRepository.findByUserIdOrderByCreatedAtDesc(userId);
        } catch (Exception e) {
            log.error("Error getting budgets for user {}: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserStats(Long userId) {
        log.debug("Getting user stats for: {}", userId);
        Map<String, Object> stats = new HashMap<>();
        
        try {
            List<Expense> userExpenses = expenseRepository.findByUserIdOrderByTransactionDateDesc(userId);
            
            BigDecimal totalExpenses = userExpenses.stream()
                .filter(e -> e.getType() == Expense.TransactionType.EXPENSE)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
            BigDecimal totalIncome = userExpenses.stream()
                .filter(e -> e.getType() == Expense.TransactionType.INCOME)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Current month expenses
            YearMonth currentMonth = YearMonth.now();
            BigDecimal currentMonthExpenses = expenseRepository.getTotalExpensesByUserAndMonth(
                userId, currentMonth.getMonthValue(), currentMonth.getYear());
            
            stats.put("totalExpenses", totalExpenses);
            stats.put("totalIncome", totalIncome);
            stats.put("currentMonthExpenses", currentMonthExpenses != null ? currentMonthExpenses : BigDecimal.ZERO);
            stats.put("balance", totalIncome.subtract(totalExpenses));
            
            return stats;
        } catch (Exception e) {
            log.error("Error calculating user stats for user {}: {}", userId, e.getMessage(), e);
            stats.put("totalExpenses", BigDecimal.ZERO);
            stats.put("totalIncome", BigDecimal.ZERO);
            stats.put("currentMonthExpenses", BigDecimal.ZERO);
            stats.put("balance", BigDecimal.ZERO);
            return stats;
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReports() {
        log.debug("Getting reports");
        Map<String, Object> reports = new HashMap<>();
        
        try {
            // User growth report
            Map<String, Long> userGrowth = getUserGrowthReport();
            
            // Expense trends
            Map<String, BigDecimal> expenseTrends = getExpenseTrendReport();
            
            // Top categories
            Map<String, BigDecimal> topCategories = getTopCategoriesReport();
            
            // System health
            Map<String, Object> systemHealth = getSystemHealthReport();
            
            reports.put("userGrowth", userGrowth);
            reports.put("expenseTrends", expenseTrends);
            reports.put("topCategories", topCategories);
            reports.put("systemHealth", systemHealth);
            
            return reports;
        } catch (Exception e) {
            log.error("Error generating reports: {}", e.getMessage(), e);
            reports.put("userGrowth", Collections.emptyMap());
            reports.put("expenseTrends", Collections.emptyMap());
            reports.put("topCategories", Collections.emptyMap());
            reports.put("systemHealth", Collections.emptyMap());
            return reports;
        }
    }

    @Transactional(readOnly = true)
    public String exportReport(String type) {
        log.debug("Exporting report of type: {}", type);
        try {
            switch (type.toLowerCase()) {
                case "users":
                    return exportUsersReport();
                case "expenses":
                    return exportExpensesReport();
                case "budgets":
                    return exportBudgetsReport();
                default:
                    return "Invalid report type";
            }
        } catch (Exception e) {
            log.error("Error exporting {} report: {}", type, e.getMessage(), e);
            return "Error generating " + type + " report: " + e.getMessage();
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSystemInfo() {
        log.debug("Getting system info");
        Map<String, Object> info = new HashMap<>();
        
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            info.put("maxMemory", maxMemory / 1024 / 1024 + " MB");
            info.put("totalMemory", totalMemory / 1024 / 1024 + " MB");
            info.put("usedMemory", usedMemory / 1024 / 1024 + " MB");
            info.put("freeMemory", freeMemory / 1024 / 1024 + " MB");
            info.put("processors", runtime.availableProcessors());
            
            // Database stats
            info.put("totalUsers", userRepository.count());
            info.put("totalExpenses", expenseRepository.count());
            info.put("totalBudgets", budgetRepository.count());
            
            // System uptime (simplified)
            info.put("serverTime", LocalDateTime.now());
            
            return info;
        } catch (Exception e) {
            log.error("Error getting system info: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    public void performSystemCleanup() {
        log.info("Starting system cleanup");
        try {
            // Clean up old verification tokens
            List<User> usersWithOldTokens = userRepository.findAll().stream()
                .filter(u -> u.getVerificationToken() != null && u.getCreatedAt().isBefore(LocalDateTime.now().minusDays(7)))
                .collect(Collectors.toList());
            
            usersWithOldTokens.forEach(user -> {
                user.setVerificationToken(null);
                userRepository.save(user);
            });
            
            // Clean up expired reset tokens
            List<User> usersWithExpiredResetTokens = userRepository.findAll().stream()
                .filter(u -> u.getResetToken() != null && 
                       u.getResetTokenExpiry() != null && 
                       u.getResetTokenExpiry().isBefore(LocalDateTime.now()))
                .collect(Collectors.toList());
            
            usersWithExpiredResetTokens.forEach(user -> {
                user.setResetToken(null);
                user.setResetTokenExpiry(null);
                userRepository.save(user);
            });
            
            log.info("System cleanup completed successfully. Cleaned {} verification tokens and {} reset tokens", 
                    usersWithOldTokens.size(), usersWithExpiredResetTokens.size());
        } catch (Exception e) {
            log.error("Error during system cleanup: {}", e.getMessage(), e);
            throw new RuntimeException("System cleanup failed: " + e.getMessage());
        }
    }

    // Private helper methods
    @Transactional(readOnly = true)
    private Map<String, BigDecimal> getMonthlyExpenseStats() {
        Map<String, BigDecimal> monthlyStats = new LinkedHashMap<>();
        
        try {
            for (int i = 5; i >= 0; i--) {
                YearMonth month = YearMonth.now().minusMonths(i);
                String key = month.getMonth().name().substring(0, 3) + " " + month.getYear();
                
                BigDecimal monthTotal = expenseRepository.findAll().stream()
                    .filter(e -> e.getType() == Expense.TransactionType.EXPENSE)
                    .filter(e -> YearMonth.from(e.getTransactionDate()).equals(month))
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                monthlyStats.put(key, monthTotal);
            }
        } catch (Exception e) {
            log.error("Error calculating monthly stats: {}", e.getMessage(), e);
        }
        
        return monthlyStats;
    }

    @Transactional(readOnly = true)
    private Map<String, BigDecimal> getCategoryStats(int month, int year) {
        try {
            List<Expense> allExpenses = expenseRepository.findAll();
            return allExpenses.stream()
                .filter(e -> e.getType() == Expense.TransactionType.EXPENSE)
                .filter(e -> e.getTransactionDate().getMonthValue() == month && e.getTransactionDate().getYear() == year)
                .collect(Collectors.groupingBy(
                    Expense::getCategory,
                    Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));
        } catch (Exception e) {
            log.error("Error calculating category stats: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    @Transactional(readOnly = true)
    private Map<String, Long> getUserGrowthReport() {
        Map<String, Long> growth = new LinkedHashMap<>();
        
        try {
            for (int i = 11; i >= 0; i--) {
                YearMonth month = YearMonth.now().minusMonths(i);
                String key = month.getMonth().name().substring(0, 3) + " " + month.getYear();
                
                long count = userRepository.findAll().stream()
                    .filter(u -> YearMonth.from(u.getCreatedAt().toLocalDate()).equals(month))
                    .count();
                
                growth.put(key, count);
            }
        } catch (Exception e) {
            log.error("Error calculating user growth: {}", e.getMessage(), e);
        }
        
        return growth;
    }

    @Transactional(readOnly = true)
    private Map<String, BigDecimal> getExpenseTrendReport() {
        return getMonthlyExpenseStats();
    }

    @Transactional(readOnly = true)
    private Map<String, BigDecimal> getTopCategoriesReport() {
        YearMonth currentMonth = YearMonth.now();
        return getCategoryStats(currentMonth.getMonthValue(), currentMonth.getYear());
    }

    @Transactional(readOnly = true)
    private Map<String, Object> getSystemHealthReport() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Check database connectivity
            userRepository.count();
            health.put("databaseStatus", "Connected");
            
            // Check memory usage
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            health.put("memoryUsage", String.format("%.2f%%", memoryUsagePercent));
            health.put("memoryStatus", memoryUsagePercent > 80 ? "Warning" : "Good");
            
        } catch (Exception e) {
            log.error("Error checking system health: {}", e.getMessage(), e);
            health.put("databaseStatus", "Error: " + e.getMessage());
            health.put("memoryUsage", "Unknown");
            health.put("memoryStatus", "Error");
        }
        
        return health;
    }

    @Transactional(readOnly = true)
    private String exportUsersReport() {
        try {
            List<User> users = userRepository.findAll();
            StringBuilder csv = new StringBuilder();
            csv.append("ID,Email,First Name,Last Name,Role,Verified,Created At\n");
            
            for (User user : users) {
                csv.append(user.getId()).append(",")
                   .append(csvEscape(user.getEmail())).append(",")
                   .append(csvEscape(user.getFirstName())).append(",")
                   .append(csvEscape(user.getLastName())).append(",")
                   .append(user.getRole()).append(",")
                   .append(user.isEmailVerified()).append(",") // This now safely handles nulls
                   .append(user.getCreatedAt()).append("\n");
            }
            
            return csv.toString();
        } catch (Exception e) {
            log.error("Error exporting users report: {}", e.getMessage(), e);
            return "Error generating users report: " + e.getMessage();
        }
    }

    @Transactional(readOnly = true)
    private String exportExpensesReport() {
        try {
            List<Expense> expenses = expenseRepository.findAll();
            StringBuilder csv = new StringBuilder();
            csv.append("ID,User Email,Title,Amount,Category,Type,Date,Description\n");
            
            for (Expense expense : expenses) {
                csv.append(expense.getId()).append(",")
                   .append(csvEscape(expense.getUser().getEmail())).append(",")
                   .append(csvEscape(expense.getTitle())).append(",")
                   .append(expense.getAmount()).append(",")
                   .append(csvEscape(expense.getCategory())).append(",")
                   .append(expense.getType()).append(",")
                   .append(expense.getTransactionDate()).append(",")
                   .append(csvEscape(expense.getDescription() != null ? expense.getDescription() : ""))
                   .append("\n");
            }
            
            return csv.toString();
        } catch (Exception e) {
            log.error("Error exporting expenses report: {}", e.getMessage(), e);
            return "Error generating expenses report: " + e.getMessage();
        }
    }

    @Transactional(readOnly = true)
    private String exportBudgetsReport() {
        try {
            List<Budget> budgets = budgetRepository.findAll();
            StringBuilder csv = new StringBuilder();
            csv.append("ID,User Email,Category,Amount,Month,Year,Created At\n");
            
            for (Budget budget : budgets) {
                csv.append(budget.getId()).append(",")
                   .append(csvEscape(budget.getUser().getEmail())).append(",")
                   .append(csvEscape(budget.getCategory())).append(",")
                   .append(budget.getAmount()).append(",")
                   .append(budget.getMonth()).append(",")
                   .append(budget.getYear()).append(",")
                   .append(budget.getCreatedAt()).append("\n");
            }
            
            return csv.toString();
        } catch (Exception e) {
            log.error("Error exporting budgets report: {}", e.getMessage(), e);
            return "Error generating budgets report: " + e.getMessage();
        }
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
