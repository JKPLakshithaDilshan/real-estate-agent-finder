package com.realestate.app.controller;

import com.realestate.app.dto.UserForm;
import com.realestate.app.model.User;
import com.realestate.app.service.AppointmentService;
import com.realestate.app.service.UserService;
import com.realestate.app.util.ValidationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {
    private final UserService userService;
    private final AppointmentService appointmentService;

    public AdminUserController(UserService userService, AppointmentService appointmentService) {
        this.userService = userService;
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public String listUsers(@RequestParam(name = "q", required = false) String query, Model model) {
        model.addAttribute("users", userService.searchUsers(query));
        model.addAttribute("query", query == null ? "" : query.trim());
        model.addAttribute("adminView", true);
        return "user/user-list";
    }

    @GetMapping("/{id}")
    public String userProfile(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        User user = userService.getUserById(id);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found.");
            return "redirect:/admin/users";
        }

        model.addAttribute("user", user);
        model.addAttribute("appointments", appointmentService.getAppointmentsByUser(user.getId()));
        model.addAttribute("adminView", true);
        return "user/user-profile";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        User user = userService.getUserById(id);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found.");
            return "redirect:/admin/users";
        }

        model.addAttribute("userForm", toForm(user));
        model.addAttribute("user", user);
        model.addAttribute("formMode", "edit");
        model.addAttribute("adminView", true);
        return "user/user-form";
    }

    @PostMapping("/{id}/update")
    public String updateUser(@PathVariable String id,
            @ModelAttribute("userForm") UserForm form,
            RedirectAttributes redirectAttributes) {
        User existing = userService.getUserById(id);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found.");
            return "redirect:/admin/users";
        }

        try {
            User updated = mergeForm(form, existing, true);
            userService.updateUser(updated);
            redirectAttributes.addFlashAttribute("toastSuccess", "User updated successfully.");
            return "redirect:/admin/users/" + id;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("toastSuccess", "User deleted successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    private UserForm toForm(User user) {
        UserForm form = new UserForm();
        form.setId(user.getId());
        form.setUsername(user.getUsername());
        form.setName(user.getName());
        form.setEmail(user.getEmail());
        form.setPhone(user.getPhone());
        form.setPassword(user.getPassword());
        form.setConfirmPassword(user.getPassword());
        form.setPreferredLocation(user.getPreferredLocation());
        form.setPropertyType(user.getPropertyType());
        return form;
    }

    private User mergeForm(UserForm form, User existing, boolean keepExistingPasswordWhenBlank) {
        User updated = new User();
        updated.setId(existing.getId());
        updated.setActive(existing.isActive());

        String normalizedUsername = ValidationUtils.normalizeRequired(form.getUsername(), "Username");
        updated.setUsername(normalizedUsername);
        updated.setEmail(ValidationUtils.normalize(form.getEmail()));
        updated.setPhone(ValidationUtils.normalize(form.getPhone()));

        String normalizedPassword = ValidationUtils.normalize(form.getPassword());
        String normalizedConfirmPassword = ValidationUtils.normalize(form.getConfirmPassword());
        if (keepExistingPasswordWhenBlank && (normalizedPassword == null || normalizedPassword.isBlank())) {
            updated.setPassword(existing.getPassword());
        } else {
            ValidationUtils.validatePasswordConfirmation(normalizedPassword, normalizedConfirmPassword);
            updated.setPassword(normalizedPassword);
        }

        updated.setPreferredLocation(ValidationUtils.normalize(form.getPreferredLocation()));
        updated.setPropertyType(ValidationUtils.normalize(form.getPropertyType()));
        return updated;
    }
}