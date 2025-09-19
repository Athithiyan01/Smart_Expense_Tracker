package com.example.Smart_Spend.controller;

import com.example.Smart_Spend.entity.User;
import com.example.Smart_Spend.entity.Expense;
import com.example.Smart_Spend.entity.Budget;
import com.example.Smart_Spend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        Map<String, Object> stats = adminService.getDashboardStats();
        model.addAllAttributes(stats);
        model.addAttribute("title", "Admin Dashboard");
        return "admin/admin-dashboard";
    }

    @GetMapping("/users")
    public String manageUsers(Model model) {
        List<User> users = adminService.getAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("title", "User Management");
        return "admin/admin-users";
    }

    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.toggleUserStatus(id);
            redirectAttributes.addFlashAttribute("message", "User status updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating user status: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteUser(id);
            redirectAttributes.addFlashAttribute("message", "User deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/{id}/details")
    public String userDetails(@PathVariable Long id, Model model) {
        try {
            User user = adminService.getUserById(id);
            List<Expense> expenses = adminService.getUserExpenses(id);
            List<Budget> budgets = adminService.getUserBudgets(id);
            Map<String, Object> userStats = adminService.getUserStats(id);

            model.addAttribute("user", user);
            model.addAttribute("expenses", expenses);
            model.addAttribute("budgets", budgets);
            model.addAttribute("userStats", userStats);
            model.addAttribute("title", "User Details - " + user.getFirstName() + " " + user.getLastName());
            return "admin/admin-user-details";
        } catch (Exception e) {
            return "redirect:/admin/users";
        }
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        Map<String, Object> reports = adminService.getReports();
        model.addAttribute("reports", reports);
        model.addAttribute("title", "System Reports");
        return "admin/reports";
    }

    @GetMapping("/reports/export")
    public ResponseEntity<String> exportReports(@RequestParam String type) {
        try {
            String csvContent = adminService.exportReport(type);
            String filename = type + "_report_" + System.currentTimeMillis() + ".csv";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(csvContent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error generating report: " + e.getMessage());
        }
    }

    @GetMapping("/system")
    public String systemInfo(Model model) {
        Map<String, Object> systemInfo = adminService.getSystemInfo();
        model.addAttribute("systemInfo", systemInfo);
        model.addAttribute("title", "System Information");
        return "admin/system";
    }

    @PostMapping("/system/cleanup")
    public String systemCleanup(RedirectAttributes redirectAttributes) {
        try {
            adminService.performSystemCleanup();
            redirectAttributes.addFlashAttribute("message", "System cleanup completed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error during cleanup: " + e.getMessage());
        }
        return "redirect:/admin/system";
    }
}