package com.realestate.app.controller;

import com.realestate.app.auth.SessionUtils;
import com.realestate.app.model.Agent;
import com.realestate.app.model.Rating;
import com.realestate.app.service.AgentService;
import com.realestate.app.service.AppointmentService;
import com.realestate.app.service.RatingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/ratings")
public class RatingController {
    private final RatingService ratingService;
    private final AgentService agentService;
    private final AppointmentService appointmentService;

    public RatingController(RatingService ratingService, AgentService agentService, AppointmentService appointmentService) {
        this.ratingService = ratingService;
        this.agentService = agentService;
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public String listRatings(
            @RequestParam(name = "agentId", required = false) String agentId,
            @RequestParam(name = "userId", required = false) String userId,
            Model model,
            HttpSession session) {
        if (SessionUtils.isUser(session)) {
            return "redirect:/ratings/my";
        }

        if (SessionUtils.isAgent(session)) {
            return "redirect:/agent/ratings";
        }

        List<Rating> ratings = ratingService.searchRatings(agentId, userId);
        model.addAttribute("ratings", ratings);
        model.addAttribute("agentId", safe(agentId));
        model.addAttribute("userId", safe(userId));
        model.addAttribute("averageRating", ratingService.calculateAverageRating(agentId));
        model.addAttribute("allowCreate", false);
        model.addAttribute("allowEdit", false);
        model.addAttribute("pageTitle", "Rating Management");
        return "rating/rating-list";
    }

    @GetMapping("/my")
    public String myRatings(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        String loggedInUserId = SessionUtils.getLoggedInUserId(session);
        if (!SessionUtils.isUser(session) || loggedInUserId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login as a user to view your ratings.");
            return "redirect:/login?next=/ratings/my";
        }

        model.addAttribute("ratings", ratingService.getRatingsByUser(loggedInUserId));
        model.addAttribute("agentId", "");
        model.addAttribute("userId", loggedInUserId);
        model.addAttribute("averageRating", 0.0);
        model.addAttribute("allowCreate", false);
        model.addAttribute("allowEdit", true);
        model.addAttribute("pageTitle", "My Ratings");
        return "rating/rating-list";
    }

    @GetMapping("/new")
    public String showRatingForm(
            @RequestParam(name = "agentId", required = false) String agentId,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String loggedInUserId = SessionUtils.getLoggedInUserId(session);
        if (!SessionUtils.isUser(session) || loggedInUserId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only logged-in users can submit ratings.");
            return "redirect:/login?next=/ratings/new?agentId=" + safe(agentId);
        }

        String selectedAgentId = safe(agentId);
        Agent selectedAgent = selectedAgentId.isBlank() ? null : agentService.getApprovedAgentById(selectedAgentId);
        if (selectedAgent == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Selected agent is unavailable.");
            return "redirect:/agents";
        }

        if (!appointmentService.hasCompletedAppointment(loggedInUserId, selectedAgentId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You can rate an agent only after a completed appointment.");
            return "redirect:/agents/" + selectedAgentId;
        }

        Rating rating = new Rating();
        rating.setAgentId(selectedAgentId);
        rating.setUserId(loggedInUserId);
        model.addAttribute("rating", rating);
        model.addAttribute("formMode", "create");
        model.addAttribute("selectedAgentName", selectedAgent != null ? selectedAgent.getName() : "Selected Agent");
        model.addAttribute("currentUserName", session == null ? null : session.getAttribute("loggedInUserName"));
        return "rating/rating-form";
    }

    @GetMapping("/add/{agentId}")
    public String aliasAddRating(@PathVariable String agentId) {
        return "redirect:/ratings/new?agentId=" + safe(agentId);
    }

    @PostMapping("/save")
    public String saveRating(
            @RequestParam("agentId") String agentId,
            @RequestParam("score") int score,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            String loggedInUserId = SessionUtils.getLoggedInUserId(session);
            if (!SessionUtils.isUser(session) || loggedInUserId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Only logged-in users can submit ratings.");
                return "redirect:/login?next=/ratings/new?agentId=" + safe(agentId);
            }

            String effectiveAgentId = safe(agentId);
            if (effectiveAgentId.isBlank()) {
                throw new IllegalArgumentException("Agent context is missing. Please open rating from an agent profile.");
            }

            if (agentService.getApprovedAgentById(effectiveAgentId) == null) {
                throw new IllegalArgumentException("Selected agent is unavailable.");
            }

            if (!appointmentService.hasCompletedAppointment(loggedInUserId, effectiveAgentId)) {
                throw new IllegalArgumentException("You can rate an agent only after a completed appointment.");
            }

            Rating rating = new Rating();
            rating.setAgentId(effectiveAgentId);
            rating.setUserId(loggedInUserId);
            rating.setScore(score);
            ratingService.saveRating(rating);
            redirectAttributes.addFlashAttribute("toastSuccess", "Rating submitted successfully.");
            return "redirect:/agents/" + effectiveAgentId;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/ratings/new?agentId=" + safe(agentId);
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        String loggedInUserId = SessionUtils.getLoggedInUserId(session);
        if (!SessionUtils.isUser(session) || loggedInUserId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only logged-in users can edit ratings.");
            return "redirect:/login?next=/ratings/" + id + "/edit";
        }

        Rating rating = ratingService.getRatingById(id);
        if (rating == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Rating not found.");
            return "redirect:/ratings/my";
        }

        if (!safe(loggedInUserId).equals(safe(rating.getUserId()))) {
            redirectAttributes.addFlashAttribute("errorMessage", "You can only edit your own rating.");
            return "redirect:/ratings/my";
        }

        Agent selectedAgent = rating.getAgentId() == null ? null : agentService.getAgentById(rating.getAgentId());

        model.addAttribute("rating", rating);
        model.addAttribute("formMode", "edit");
        model.addAttribute("selectedAgentName", selectedAgent != null ? selectedAgent.getName() : "Selected Agent");
        model.addAttribute("currentUserName", session == null ? null : session.getAttribute("loggedInUserName"));
        return "rating/rating-form";
    }

    @GetMapping("/edit/{id}")
    public String aliasEditRating(@PathVariable String id) {
        return "redirect:/ratings/" + safe(id) + "/edit";
    }

    @PostMapping("/{id}/update")
    public String updateRating(@PathVariable String id,
            @RequestParam("agentId") String agentId,
            @RequestParam("score") int score,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String loggedInUserId = SessionUtils.getLoggedInUserId(session);
        if (!SessionUtils.isUser(session) || loggedInUserId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only logged-in users can update ratings.");
            return "redirect:/login?next=/ratings/" + id + "/edit";
        }

        Rating existing = ratingService.getRatingById(id);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Rating not found.");
            return "redirect:/ratings/my";
        }

        try {
            if (!safe(loggedInUserId).equals(safe(existing.getUserId()))) {
                redirectAttributes.addFlashAttribute("errorMessage", "You can only edit your own rating.");
                return "redirect:/ratings/my";
            }

            String effectiveAgentId = safe(agentId).isBlank() ? safe(existing.getAgentId()) : safe(agentId);
            existing.setAgentId(effectiveAgentId);
            existing.setUserId(loggedInUserId);
            existing.setScore(score);
            ratingService.updateRatingSecure(existing, loggedInUserId, false);
            redirectAttributes.addFlashAttribute("toastSuccess", "Rating updated successfully.");
            return "redirect:/agents/" + effectiveAgentId;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/ratings/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteRating(@PathVariable String id,
            @RequestParam(name = "agentId", required = false) String agentId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String loggedInUserId = SessionUtils.getLoggedInUserId(session);
        if (!SessionUtils.isUser(session) && !SessionUtils.isAdmin(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to continue.");
            return "redirect:/login";
        }

        try {
            ratingService.deleteRatingSecure(id, loggedInUserId, SessionUtils.isAdmin(session));
            redirectAttributes.addFlashAttribute("toastSuccess", "Rating deleted.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        if (SessionUtils.isAdmin(session) && agentId != null && !agentId.isBlank()) {
            return "redirect:/ratings?agentId=" + agentId;
        }
        return "redirect:/ratings/my";
    }

    @PostMapping("/delete/{id}")
    public String aliasDeleteRating(@PathVariable String id,
            @RequestParam(name = "agentId", required = false) String agentId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        return deleteRating(id, agentId, session, redirectAttributes);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
