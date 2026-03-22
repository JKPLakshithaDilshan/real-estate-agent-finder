package com.realestate.app.controller;

import com.realestate.app.auth.SessionUtils;
import com.realestate.app.dto.UserForm;
import com.realestate.app.model.User;
import com.realestate.app.service.AppointmentService;
import com.realestate.app.service.RatingService;
import com.realestate.app.service.ReviewService;
import com.realestate.app.service.UserService;
import com.realestate.app.util.ValidationUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {
    private final UserService userService;
    private final AppointmentService appointmentService;
    private final ReviewService reviewService;
    private final RatingService ratingService;

    public UserController(UserService userService,
            AppointmentService appointmentService,
            ReviewService reviewService,
            RatingService ratingService) {
        this.userService = userService;
        this.appointmentService = appointmentService;
        this.reviewService = reviewService;
        this.ratingService = ratingService;
    }

    @GetMapping("/users")
    public String redirectUsersRoot(HttpSession session) {
        if (SessionUtils.isAdmin(session)) {
            return "redirect:/admin/users";
        }
        if (SessionUtils.isUser(session)) {
            return "redirect:/account";
        }
        return "redirect:/login";
    }

    @GetMapping("/users/new")
    public String redirectLegacyRegister() {
        return "redirect:/register";
    }

    @PostMapping("/users/register")
    public String registerUser(@ModelAttribute("userForm") UserForm form, RedirectAttributes redirectAttributes) {
        try {
            User user = mergeForm(form, null, false);
            userService.saveUser(user);
            redirectAttributes.addFlashAttribute("toastSuccess", "Registration successful. Please sign in.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/account")
    public String accountProfile(Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to access your account.");
            return "redirect:/login";
        }

        model.addAttribute("user", currentUser);
        model.addAttribute("appointments", appointmentService.getAppointmentsByUser(currentUser.getId()));
        model.addAttribute("reviews", reviewService.getReviewsByUser(currentUser.getId()));
        model.addAttribute("ratings", ratingService.getRatingsByUser(currentUser.getId()));
        model.addAttribute("adminView", false);
        return "user/user-profile";
    }

    @GetMapping("/account/edit")
    public String showEditProfile(Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to access your account.");
            return "redirect:/login";
        }

        model.addAttribute("userForm", toForm(currentUser));
        model.addAttribute("user", currentUser);
        model.addAttribute("formMode", "edit");
        model.addAttribute("adminView", false);
        return "user/user-form";
    }

    @PostMapping("/account/update")
    public String updateProfile(@ModelAttribute("userForm") UserForm form,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to continue.");
            return "redirect:/login";
        }

        try {
            User updated = mergeForm(form, currentUser, true);
            userService.updateUser(updated);
            SessionUtils.refreshUserSession(session, updated);
            redirectAttributes.addFlashAttribute("toastSuccess", "Profile updated successfully.");
            return "redirect:/account";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/account/edit";
        }
    }

    @PostMapping("/account/delete")
    public String deleteAccount(RedirectAttributes redirectAttributes, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to continue.");
            return "redirect:/login";
        }

        try {
            userService.deleteUser(currentUser.getId());
            if (session != null) {
                session.invalidate();
            }
            redirectAttributes.addFlashAttribute("toastSuccess", "Your account has been deleted successfully.");
            return "redirect:/login";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/account";
        }
    }

    @GetMapping("/users/{id}")
    public String redirectLegacyProfile(@PathVariable String id, RedirectAttributes redirectAttributes, HttpSession session) {
        if (SessionUtils.isAdmin(session)) {
            return "redirect:/admin/users/" + id;
        }
        if (!SessionUtils.isUser(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to access your account.");
            return "redirect:/login";
        }
        return redirectOwnedRoute(id, redirectAttributes, session, "/account");
    }

    @GetMapping("/users/{id}/edit")
    public String redirectLegacyEdit(@PathVariable String id, RedirectAttributes redirectAttributes, HttpSession session) {
        if (SessionUtils.isAdmin(session)) {
            return "redirect:/admin/users/" + id + "/edit";
        }
        if (!SessionUtils.isUser(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to access your account.");
            return "redirect:/login";
        }
        return redirectOwnedRoute(id, redirectAttributes, session, "/account/edit");
    }

    @PostMapping("/users/{id}/update")
    public String redirectLegacyUpdate(@PathVariable String id, RedirectAttributes redirectAttributes, HttpSession session) {
        if (SessionUtils.isAdmin(session)) {
            return "redirect:/admin/users/" + id + "/edit";
        }
        if (!SessionUtils.isUser(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to continue.");
            return "redirect:/login";
        }
        return redirectOwnedRoute(id, redirectAttributes, session, "/account/edit");
    }

    @PostMapping("/users/{id}/delete")
    public String redirectLegacyDelete(@PathVariable String id, RedirectAttributes redirectAttributes, HttpSession session) {
        if (SessionUtils.isAdmin(session)) {
            return "redirect:/admin/users";
        }
        if (!SessionUtils.isUser(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to continue.");
            return "redirect:/login";
        }
        return redirectOwnedRoute(id, redirectAttributes, session, "/account");
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

    private User mergeForm(UserForm form, User base, boolean keepExistingPasswordWhenBlank) {
        User user = new User();
        if (base != null) {
            user.setId(base.getId());
            user.setActive(base.isActive());
        }

        String normalizedUsername = ValidationUtils.normalizeRequired(form.getUsername(), "Username");
        user.setUsername(normalizedUsername);
        user.setEmail(ValidationUtils.normalize(form.getEmail()));
        user.setPhone(ValidationUtils.normalize(form.getPhone()));

        String normalizedPassword = ValidationUtils.normalize(form.getPassword());
        String normalizedConfirmPassword = ValidationUtils.normalize(form.getConfirmPassword());
        if (keepExistingPasswordWhenBlank && base != null && (normalizedPassword == null || normalizedPassword.isBlank())) {
            user.setPassword(base.getPassword());
        } else {
            ValidationUtils.validatePasswordConfirmation(normalizedPassword, normalizedConfirmPassword);
            user.setPassword(normalizedPassword);
        }

        user.setPreferredLocation(ValidationUtils.normalize(form.getPreferredLocation()));
        user.setPropertyType(ValidationUtils.normalize(form.getPropertyType()));
        return user;
    }

    private User getCurrentUser(HttpSession session) {
        String loggedInUserId = SessionUtils.getLoggedInUserId(session);
        if (loggedInUserId == null || !SessionUtils.isUser(session)) {
            return null;
        }
        return userService.getUserById(loggedInUserId);
    }

    private String redirectOwnedRoute(String requestedId, RedirectAttributes redirectAttributes, HttpSession session, String targetRoute) {
        String loggedInUserId = SessionUtils.getLoggedInUserId(session);
        if (loggedInUserId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to access your account.");
            return "redirect:/login";
        }
        if (!loggedInUserId.equals(requestedId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You can only access your own account.");
        }
        return "redirect:" + targetRoute;
    }
}
