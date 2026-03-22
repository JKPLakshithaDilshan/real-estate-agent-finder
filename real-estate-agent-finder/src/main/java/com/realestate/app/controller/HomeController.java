package com.realestate.app.controller;

import com.realestate.app.auth.SessionUtils;
import com.realestate.app.model.Agent;
import com.realestate.app.model.Rating;
import com.realestate.app.model.Review;
import com.realestate.app.model.User;
import com.realestate.app.service.AgentService;
import com.realestate.app.service.AppointmentService;
import com.realestate.app.service.RatingService;
import com.realestate.app.service.ReviewService;
import com.realestate.app.service.UserService;
import com.realestate.app.util.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
public class HomeController {
    private final UserService userService;
    private final AgentService agentService;
    private final ReviewService reviewService;
    private final AppointmentService appointmentService;
    private final RatingService ratingService;

    public HomeController(UserService userService,
            AgentService agentService,
            ReviewService reviewService,
            AppointmentService appointmentService,
            RatingService ratingService) {
        this.userService = userService;
        this.agentService = agentService;
    this.reviewService = reviewService;
        this.appointmentService = appointmentService;
        this.ratingService = ratingService;
    }

    @GetMapping("/")
    public String home(Model model) {
    List<Agent> approvedAgents = agentService.getApprovedAgents();
    List<Rating> allRatings = ratingService.getAllRatings();

    List<Agent> topRatedAgents = agentService.getTopRatedAgents().stream()
        .limit(3)
        .toList();

    List<Agent> featuredAgents = approvedAgents.stream()
        .sorted(Comparator.comparingInt(Agent::getTotalRatings).reversed()
            .thenComparing(Comparator.comparingDouble(Agent::getAverageRating).reversed())
            .thenComparing(Agent::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
        .limit(3)
        .toList();

    List<Review> topReviews = reviewService.getAllReviews().stream()
        .filter(review -> !review.isFlagged())
        .sorted(Comparator.comparing(Review::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Review::getId, Comparator.nullsLast(Comparator.reverseOrder())))
        .limit(3)
        .toList();

    Map<String, String> reviewerNames = new LinkedHashMap<>();
    for (Review review : topReviews) {
        if (review.getUserId() == null || review.getUserId().isBlank()) {
        continue;
        }
        User user = userService.getUserById(review.getUserId());
        reviewerNames.put(review.getUserId(), user != null ? user.getUsername() : review.getUserId());
    }

        int metricAgents = approvedAgents.size();
        int metricAppointments = appointmentService.getAllAppointments().size();
        double metricAvgRating = Math.round(allRatings.stream().mapToInt(Rating::getScore).average().orElse(0.0) * 10.0) / 10.0;
        int metricSatisfaction = allRatings.isEmpty()
                ? 98
                : (int) Math.round((allRatings.stream().filter(rating -> rating.getScore() >= 4).count() * 100.0)
                        / allRatings.size());

        model.addAttribute("pageTitle", "Real Estate Agent Finder");
    model.addAttribute("featuredAgents", featuredAgents);
    model.addAttribute("topRatedAgents", topRatedAgents);
    model.addAttribute("topReviews", topReviews);
    model.addAttribute("reviewerNames", reviewerNames);
        model.addAttribute("metricAgents", metricAgents);
        model.addAttribute("metricAppointments", metricAppointments);
        model.addAttribute("metricAvgRating", metricAvgRating);
        model.addAttribute("metricSatisfaction", metricSatisfaction);
        return "index";
    }

    @GetMapping("/about")
    public String about(Model model) {
        int approvedAgents = agentService.getApprovedAgents().size();
        int users = userService.getAllUsers().size();
        int appointments = appointmentService.getAllAppointments().size();
        int reviews = reviewService.getAllReviews().size();

        model.addAttribute("pageTitle", "About Us | Real Estate Agent Finder");
        model.addAttribute("approvedAgents", approvedAgents);
        model.addAttribute("users", users);
        model.addAttribute("appointments", appointments);
        model.addAttribute("reviews", reviews);
        return "about";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("pageTitle", "Contact Us | Real Estate Agent Finder");
        model.addAttribute("approvedAgents", agentService.getApprovedAgents().size());
        model.addAttribute("appointments", appointmentService.getAllAppointments().size());
        model.addAttribute("reviews", reviewService.getAllReviews().size());
        return "contact";
    }

    @PostMapping("/contact")
    public String submitContact(@RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("subject") String subject,
            @RequestParam("message") String message,
            RedirectAttributes redirectAttributes) {
        try {
            ValidationUtils.normalizeRequired(name, "Name");
            String normalizedEmail = ValidationUtils.normalizeRequired(email, "Email");
            ValidationUtils.normalizeRequired(subject, "Subject");
            ValidationUtils.normalizeRequired(message, "Message");

            if (!normalizedEmail.contains("@") || normalizedEmail.startsWith("@") || normalizedEmail.endsWith("@")) {
                throw new IllegalArgumentException("Enter a valid email address.");
            }

            redirectAttributes.addFlashAttribute("toastSuccess", "Thanks for contacting us. Our team will get back to you soon.");
            return "redirect:/contact";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/contact";
        }
    }

    @GetMapping("/login")
    public String login(@RequestParam(name = "next", required = false) String next, Model model) {
        model.addAttribute("next", sanitizeNext(next));
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam("identifier") String identifier,
            @RequestParam("password") String password,
            @RequestParam(name = "next", required = false) String next,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        String normalizedIdentifier = ValidationUtils.normalize(identifier);
        String normalizedPassword = ValidationUtils.normalize(password);

        User user = userService.findByLoginIdentifier(normalizedIdentifier);
        if (user != null && Objects.equals(user.getPassword(), normalizedPassword)) {
            if (!user.isActive()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Your account is deactivated.");
                return "redirect:/login";
            }

            SessionUtils.startUserSession(request, user);
            redirectAttributes.addFlashAttribute("toastSuccess", "Login successful.");
            String target = sanitizeNext(next);
            return "redirect:" + (target == null ? "/account" : target);
        }

        Agent agent = findAgentByIdentifier(normalizedIdentifier);
        if (agent != null && Objects.equals(agent.getPassword(), normalizedPassword)) {
            if (!Agent.STATUS_APPROVED.equals(agent.getApprovalStatus())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Your agent profile is not approved yet.");
                return "redirect:/login";
            }

            SessionUtils.startAgentSession(request, agent);
            redirectAttributes.addFlashAttribute("toastSuccess", "Agent login successful.");
            String target = sanitizeNext(next);
            return "redirect:" + (target == null ? "/agent/dashboard" : target);
        }

        redirectAttributes.addFlashAttribute("errorMessage", "Invalid username/email or password.");
        return "redirect:/login";
    }

    private String sanitizeNext(String next) {
        String normalized = ValidationUtils.normalize(next);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        if (!normalized.startsWith("/") || normalized.startsWith("//") || normalized.contains("://")) {
            return null;
        }
        return normalized;
    }

    private Agent findAgentByIdentifier(String identifier) {
        Agent byEmail = agentService.getAgentByEmail(identifier);
        if (byEmail != null) {
            return byEmail;
        }
        return agentService.getAgentByUsername(identifier);
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {
        try {
            User user = new User();
            String normalizedUsername = ValidationUtils.normalizeRequired(username, "Username");
            user.setUsername(normalizedUsername);
            user.setEmail(ValidationUtils.normalize(email));
            user.setPhone(ValidationUtils.normalize(phone));
            user.setPassword(ValidationUtils.normalize(password));

            ValidationUtils.validatePasswordConfirmation(password, confirmPassword);

            userService.saveUser(user);
            redirectAttributes.addFlashAttribute("toastSuccess", "Registration successful. Please sign in.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        if (session != null) {
            session.invalidate();
        }
        redirectAttributes.addFlashAttribute("toastSuccess", "You have been logged out.");
        return "redirect:/login";
    }
}
