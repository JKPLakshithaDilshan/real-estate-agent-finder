package com.realestate.app.controller;

import com.realestate.app.auth.SessionUtils;
import com.realestate.app.model.Admin;
import com.realestate.app.service.AdminService;
import com.realestate.app.service.AgentService;
import com.realestate.app.service.AppointmentService;
import com.realestate.app.service.RatingService;
import com.realestate.app.service.ReviewService;
import com.realestate.app.service.UserService;
import com.realestate.app.util.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;
    private final AgentService agentService;
    private final UserService userService;
    private final AppointmentService appointmentService;
    private final RatingService ratingService;
    private final ReviewService reviewService;

    public AdminController(AdminService adminService,
            AgentService agentService,
            UserService userService,
            AppointmentService appointmentService,
            RatingService ratingService,
            ReviewService reviewService) {
        this.adminService = adminService;
        this.agentService = agentService;
        this.userService = userService;
        this.appointmentService = appointmentService;
        this.ratingService = ratingService;
        this.reviewService = reviewService;
    }

    @GetMapping("/login")
    public String showAdminLogin() {
        return "admin/login";
    }

    @PostMapping("/login")
    public String adminLogin(@RequestParam("email") String email,
            @RequestParam("password") String password,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        String normalizedEmail = ValidationUtils.normalize(email);
        String normalizedPassword = ValidationUtils.normalize(password);

        Admin admin = adminService.getAdminByEmail(normalizedEmail);
        if (admin == null || !Objects.equals(admin.getPassword(), normalizedPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid admin credentials.");
            return "redirect:/admin/login";
        }

        SessionUtils.startAdminSession(request, admin);
        redirectAttributes.addFlashAttribute("toastSuccess", "Admin login successful.");
        return "redirect:/admin/dashboard";
    }

    @GetMapping({"", "/dashboard"})
    public String dashboard(Model model) {
        int adminCount = adminService.getAllAdmins().size();
        int agentCount = agentService.getAllAgents().size();
        int userCount = userService.getAllUsers().size();
        List<com.realestate.app.model.Appointment> appointments = appointmentService.getAllAppointments();
        int appointmentCount = appointments.size();
        int ratingCount = ratingService.getAllRatings().size();
        int reviewCount = reviewService.getAllReviews().size();

        int moduleTotal = Math.max(1, adminCount + agentCount + userCount + appointmentCount + ratingCount + reviewCount);
        double adminPct = (adminCount * 100.0) / moduleTotal;
        double agentPct = (agentCount * 100.0) / moduleTotal;
        double userPct = (userCount * 100.0) / moduleTotal;
        double appointmentPct = (appointmentCount * 100.0) / moduleTotal;
        double ratingPct = (ratingCount * 100.0) / moduleTotal;
        double reviewPct = (reviewCount * 100.0) / moduleTotal;

        long pendingCount = appointments.stream().filter(a -> com.realestate.app.model.Appointment.STATUS_PENDING.equals(a.getStatus())).count();
        long confirmedCount = appointments.stream().filter(a -> com.realestate.app.model.Appointment.STATUS_CONFIRMED.equals(a.getStatus())).count();
        long completedCount = appointments.stream().filter(a -> com.realestate.app.model.Appointment.STATUS_COMPLETED.equals(a.getStatus())).count();
        long cancelledCount = appointments.stream().filter(a -> com.realestate.app.model.Appointment.STATUS_CANCELLED.equals(a.getStatus())).count();

        double flowTotal = Math.max(1, pendingCount + confirmedCount + completedCount + cancelledCount);
        double pendingPct = (pendingCount * 100.0) / flowTotal;
        double confirmedPct = (confirmedCount * 100.0) / flowTotal;
        double completedPct = (completedCount * 100.0) / flowTotal;
        double cancelledPct = (cancelledCount * 100.0) / flowTotal;

        model.addAttribute("adminCount", adminCount);
        model.addAttribute("agentCount", agentCount);
        model.addAttribute("userCount", userCount);
        model.addAttribute("appointmentCount", appointmentCount);
        model.addAttribute("ratingCount", ratingCount);
        model.addAttribute("reviewCount", reviewCount);

        model.addAttribute("adminPct", adminPct);
        model.addAttribute("agentPct", agentPct);
        model.addAttribute("userPct", userPct);
        model.addAttribute("appointmentPct", appointmentPct);
        model.addAttribute("ratingPct", ratingPct);
        model.addAttribute("reviewPct", reviewPct);

        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("confirmedCount", confirmedCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("cancelledCount", cancelledCount);
        model.addAttribute("pendingPct", pendingPct);
        model.addAttribute("confirmedPct", confirmedPct);
        model.addAttribute("completedPct", completedPct);
        model.addAttribute("cancelledPct", cancelledPct);
        return "admin/dashboard";
    }

    @GetMapping("/list")
    public String listAdmins(@RequestParam(name = "q", required = false) String query, Model model) {
        model.addAttribute("admins", adminService.searchAdmins(query));
        model.addAttribute("query", query == null ? "" : query.trim());
        return "admin/admin-list";
    }

    @GetMapping("/new")
    public String showNewForm(Model model) {
        model.addAttribute("admin", new Admin());
        model.addAttribute("formMode", "create");
        return "admin/admin-form";
    }

    @PostMapping("/save")
    public String saveAdmin(@ModelAttribute("admin") Admin admin,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {
        try {
            ValidationUtils.validatePasswordConfirmation(admin.getPassword(), confirmPassword);
            adminService.saveAdmin(admin);
            redirectAttributes.addFlashAttribute("toastSuccess", "Admin registered successfully.");
            return "redirect:/admin/list";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Admin admin = adminService.getAdminById(id);
        if (admin == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Admin not found.");
            return "redirect:/admin/list";
        }

        model.addAttribute("admin", admin);
        model.addAttribute("formMode", "edit");
        return "admin/admin-form";
    }

    @PostMapping("/{id}/update")
    public String updateAdmin(@PathVariable String id,
            @ModelAttribute("admin") Admin formAdmin,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {
        Admin existing = adminService.getAdminById(id);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Admin not found.");
            return "redirect:/admin/list";
        }

        try {
            ValidationUtils.validatePasswordConfirmation(formAdmin.getPassword(), confirmPassword);

            existing.setName(formAdmin.getName());
            existing.setEmail(formAdmin.getEmail());
            existing.setPhone(formAdmin.getPhone());
            existing.setPassword(formAdmin.getPassword());
            existing.setAdminLevel(formAdmin.getAdminLevel());

            adminService.updateAdmin(existing);
            redirectAttributes.addFlashAttribute("toastSuccess", "Admin updated successfully.");
            return "redirect:/admin/list";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteAdmin(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteAdmin(id);
            redirectAttributes.addFlashAttribute("toastSuccess", "Admin deleted successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/list";
    }
}
