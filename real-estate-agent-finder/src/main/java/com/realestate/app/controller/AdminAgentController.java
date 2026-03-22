package com.realestate.app.controller;

import com.realestate.app.model.Agent;
import com.realestate.app.model.Review;
import com.realestate.app.model.User;
import com.realestate.app.service.AgentService;
import com.realestate.app.service.AppointmentService;
import com.realestate.app.service.RatingService;
import com.realestate.app.service.ReviewService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/agents")
public class AdminAgentController {
    private final AgentService agentService;
    private final ReviewService reviewService;
    private final RatingService ratingService;
    private final AppointmentService appointmentService;
    private final UserService userService;

    public AdminAgentController(AgentService agentService,
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
        model.addAttribute("agents", agentService.searchAgents(query));
        model.addAttribute("query", query == null ? "" : query.trim());
        model.addAttribute("adminView", true);
        return "agent/agent-list";
    }

    @GetMapping("/pending")
    public String pendingAgents(Model model) {
        model.addAttribute("agents", agentService.getPendingAgents());
        return "admin/agent-pending-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("agent", new Agent());
        model.addAttribute("formMode", "create");
        model.addAttribute("adminView", true);
        return "agent/agent-form";
    }

    @PostMapping("/save")
    public String createAgent(@ModelAttribute("agent") Agent agent,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String filename = agentService.saveProfileImage(imageFile);
                agent.setProfileImage(filename);
            }
            agentService.saveAgentByAdmin(agent);
            redirectAttributes.addFlashAttribute("toastSuccess", "Agent created successfully.");
            return "redirect:/admin/agents";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("agent", agent);
            model.addAttribute("formMode", "create");
            model.addAttribute("adminView", true);
            return "agent/agent-form";
        }
    }

    @GetMapping("/{id}")
    public String viewAgentDetails(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Agent agent = agentService.getAgentById(id);
        if (agent == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Agent not found.");
            return "redirect:/admin/agents";
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
        model.addAttribute("adminView", true);
        return "agent/agent-details";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Agent agent = agentService.getAgentById(id);
        if (agent == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Agent not found.");
            return "redirect:/admin/agents";
        }

        model.addAttribute("agent", agent);
        model.addAttribute("formMode", "edit");
        model.addAttribute("adminView", true);
        return "agent/agent-form";
    }

    @PostMapping("/{id}/update")
    public String updateAgent(@PathVariable String id,
            @ModelAttribute("agent") Agent formAgent,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {
        Agent existing = agentService.getAgentById(id);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Agent not found.");
            return "redirect:/admin/agents";
        }

        try {
            existing.setName(formAgent.getName());
            existing.setUsername(formAgent.getUsername());
            existing.setEmail(formAgent.getEmail());
            existing.setPhone(formAgent.getPhone());
            existing.setAddress(formAgent.getAddress());
            existing.setNicNumber(formAgent.getNicNumber());
            if (ValidationUtils.normalize(formAgent.getPassword()) != null
                    && !ValidationUtils.normalize(formAgent.getPassword()).isBlank()) {
                existing.setPassword(formAgent.getPassword());
            }
            existing.setSpecialization(formAgent.getSpecialization());
            existing.setLocation(formAgent.getLocation());
            existing.setYearsOfExperience(formAgent.getYearsOfExperience());
            existing.setBio(formAgent.getBio());
            if (imageFile != null && !imageFile.isEmpty()) {
                String filename = agentService.saveProfileImage(imageFile);
                existing.setProfileImage(filename);
            }
            // If no new file submitted, existing.profileImage is already set from the loaded record

            agentService.updateAgent(existing);
            redirectAttributes.addFlashAttribute("toastSuccess", "Agent updated successfully.");
            return "redirect:/admin/agents";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/agents/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteAgent(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            agentService.deleteAgent(id);
            redirectAttributes.addFlashAttribute("toastSuccess", "Agent deleted successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/agents";
    }

    @PostMapping("/approve/{id}")
    public String approveAgent(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            agentService.approveAgent(id);
            redirectAttributes.addFlashAttribute("toastSuccess", "Agent approved successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/agents/pending";
    }

    @PostMapping("/reject/{id}")
    public String rejectAgent(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            agentService.rejectAgent(id);
            redirectAttributes.addFlashAttribute("toastSuccess", "Agent rejected successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/agents/pending";
    }
}