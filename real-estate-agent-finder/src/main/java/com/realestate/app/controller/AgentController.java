package com.realestate.app.controller;

import com.realestate.app.auth.SessionUtils;
import com.realestate.app.model.Agent;
import com.realestate.app.model.Review;
import com.realestate.app.model.User;
import com.realestate.app.service.AgentService;
import com.realestate.app.service.AppointmentService;
import com.realestate.app.service.RatingService;
import com.realestate.app.service.ReviewService;
import com.realestate.app.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/agents")
public class AgentController {
    private final AgentService agentService;
    private final ReviewService reviewService;
    private final RatingService ratingService;
    private final AppointmentService appointmentService;
    private final UserService userService;

    public AgentController(AgentService agentService,
            ReviewService reviewService,
            RatingService ratingService,
            AppointmentService appointmentService,
            UserService userService) {
        this.agentService = agentService;
        this.reviewService = reviewService;
        this.ratingService = ratingService;
        this.appointmentService = appointmentService;
        this.userService = userService;
    }

    @GetMapping
    public String listAgents(@RequestParam(name = "q", required = false) String query, Model model) {
        List<Agent> agents = agentService.searchApprovedAgents(query);
        model.addAttribute("agents", agents);
        model.addAttribute("query", query == null ? "" : query.trim());
        model.addAttribute("adminView", false);
        return "agent/agent-list";
    }

    @GetMapping("/apply")
    public String showApplicationForm(Model model) {
        if (!model.containsAttribute("agent")) {
            model.addAttribute("agent", new Agent());
        }
        model.addAttribute("formMode", "apply");
        model.addAttribute("adminView", false);
        return "agent/agent-form";
    }

    @PostMapping("/apply")
    public String submitApplication(@ModelAttribute("agent") Agent agent,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String filename = agentService.saveProfileImage(imageFile);
                agent.setProfileImage(filename);
            }
            agentService.saveAgentApplication(agent);
            redirectAttributes.addFlashAttribute("toastSuccess", "Your profile has been submitted and is awaiting admin approval.");
            return "redirect:/agents/apply/success/" + agent.getId();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("agent", agent);
            model.addAttribute("formMode", "apply");
            model.addAttribute("adminView", false);
            return "agent/agent-form";
        }
    }

    @GetMapping("/apply/success/{id}")
    public String applicationSuccess(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Agent agent = agentService.getAgentById(id);
        if (agent == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Agent application not found.");
            return "redirect:/agents/apply";
        }

        model.addAttribute("agent", agent);
        return "agent/application-submitted";
    }

    @GetMapping("/{id}")
    public String viewAgentDetails(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Agent agent = agentService.getApprovedAgentById(id);
        if (agent == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Agent not found or not approved yet.");
            return "redirect:/agents";
        }

        model.addAttribute("agent", agent);
        List<Review> reviews = reviewService.getReviewsByAgent(id);
        Map<String, String> reviewerNames = new LinkedHashMap<>();
        for (Review review : reviews) {
            if (review.getUserId() == null || review.getUserId().isBlank()) {
                continue;
            }
            User user = userService.getUserById(review.getUserId());
            reviewerNames.put(review.getUserId(), user != null ? user.getUsername() : review.getUserId());
        }

        model.addAttribute("reviews", reviews);
        model.addAttribute("reviewerNames", reviewerNames);
        model.addAttribute("ratings", ratingService.getRatingsByAgent(id));
        model.addAttribute("appointments", appointmentService.getAppointmentsByAgent(id));
        model.addAttribute("averageRating", ratingService.calculateAverageRating(id));
        model.addAttribute("adminView", false);
        return "agent/agent-details";
    }

    @GetMapping("/new")
    public String redirectLegacyCreate(HttpSession session, RedirectAttributes redirectAttributes) {
        if (SessionUtils.isAdmin(session)) {
            return "redirect:/admin/agents/new";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "Please submit an application using the public apply page.");
        return "redirect:/agents/apply";
    }

    @GetMapping("/{id}/edit")
    public String redirectLegacyEdit(@PathVariable String id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (SessionUtils.isAdmin(session)) {
            return "redirect:/admin/agents/" + id + "/edit";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "Only admins can manage agent profiles.");
        return "redirect:/agents";
    }

    @PostMapping("/{id}/update")
    public String redirectLegacyUpdate(@PathVariable String id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (SessionUtils.isAdmin(session)) {
            return "redirect:/admin/agents/" + id + "/edit";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "Only admins can manage agent profiles.");
        return "redirect:/agents";
    }

    @PostMapping("/{id}/delete")
    public String redirectLegacyDelete(@PathVariable String id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (SessionUtils.isAdmin(session)) {
            return "redirect:/admin/agents";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "Only admins can manage agent profiles.");
        return "redirect:/agents";
    }

    @GetMapping("/top-rated")
    public String topRatedAgents(Model model) {
        model.addAttribute("agents", agentService.getTopRatedAgents());
        return "agent/top-rated-agents";
    }

    @GetMapping("/sort/rating")
    public String topRatedAgentsAlias(Model model) {
        return topRatedAgents(model);
    }
}
