package com.realestate.app.controller;

import com.realestate.app.auth.SessionUtils;
import com.realestate.app.model.Agent;
import com.realestate.app.model.Review;
import com.realestate.app.service.AgentService;
import com.realestate.app.service.AppointmentService;
import com.realestate.app.service.ReviewService;
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
@RequestMapping("/reviews")
public class ReviewController {
    private final ReviewService reviewService;
    private final AgentService agentService;
    private final AppointmentService appointmentService;

    public ReviewController(ReviewService reviewService, AgentService agentService, AppointmentService appointmentService) {
        this.reviewService = reviewService;
        this.agentService = agentService;
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public String listReviews(
            @RequestParam(name = "agentId", required = false) String agentId,
            @RequestParam(name = "userId", required = false) String userId,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (SessionUtils.isUser(session)) {
            return "redirect:/reviews/my";
        }

        if (SessionUtils.isAgent(session)) {
            return "redirect:/agent/reviews";
        }

        List<Review> reviews = reviewService.searchReviews(agentId, userId);
        model.addAttribute("reviews", reviews);
        model.addAttribute("agentId", safe(agentId));
        model.addAttribute("userId", safe(userId));
        model.addAttribute("allowCreate", false);
        model.addAttribute("allowEdit", false);
        model.addAttribute("pageTitle", "Review Management");
        if (redirectAttributes != null) {
            // no-op to keep method signature aligned with other protected routes
        }
        return "review/review-list";
    }

    @GetMapping("/my")
    public String myReviews(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        String loggedInUserId = SessionUtils.getLoggedInUserId(session);
        if (!SessionUtils.isUser(session) || loggedInUserId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login as a user to view your reviews.");
            return "redirect:/login?next=/reviews/my";
        }

        model.addAttribute("reviews", reviewService.getReviewsByUser(loggedInUserId));
        model.addAttribute("agentId", "");
        model.addAttribute("userId", loggedInUserId);
        model.addAttribute("allowCreate", false);
        model.addAttribute("allowEdit", true);
        model.addAttribute("pageTitle", "My Reviews");
        return "review/review-list";
    }

    @GetMapping("/new")
    public String showReviewForm(
            @RequestParam(name = "agentId", required = false) String agentId,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String loggedInUserId = SessionUtils.getLoggedInUserId(session);
        if (!SessionUtils.isUser(session) || loggedInUserId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only logged-in users can submit reviews.");
            return "redirect:/login?next=/reviews/new?agentId=" + safe(agentId);
        }

        String selectedAgentId = safe(agentId);
        Agent selectedAgent = selectedAgentId.isBlank() ? null : agentService.getApprovedAgentById(selectedAgentId);
        if (selectedAgent == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Selected agent is unavailable.");
            return "redirect:/agents";
        }

        if (!appointmentService.hasCompletedAppointment(loggedInUserId, selectedAgentId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You can review an agent only after a completed appointment.");
            return "redirect:/agents/" + selectedAgentId;
        }

        Review review = new Review();
        review.setAgentId(selectedAgentId);
        review.setUserId(loggedInUserId);
        model.addAttribute("review", review);
        model.addAttribute("formMode", "create");
        model.addAttribute("selectedAgentName", selectedAgent != null ? selectedAgent.getName() : "Selected Agent");
        model.addAttribute("currentUserName", session == null ? null : session.getAttribute("loggedInUserName"));
        return "review/review-form";
    }

    @GetMapping("/add/{agentId}")
    public String aliasAddReview(@PathVariable String agentId) {
        return "redirect:/reviews/new?agentId=" + safe(agentId);
    }

    @PostMapping("/save")
    public String saveReview(
            @RequestParam("agentId") String agentId,
            @RequestParam("comment") String comment,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            String loggedInUserId = SessionUtils.getLoggedInUserId(session);
            if (!SessionUtils.isUser(session) || loggedInUserId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Only logged-in users can submit reviews.");
                return "redirect:/login?next=/reviews/new?agentId=" + safe(agentId);
            }

            String effectiveAgentId = safe(agentId);
            if (effectiveAgentId.isBlank()) {
                throw new IllegalArgumentException("Agent context is missing. Please open review from an agent profile.");
            }

            if (agentService.getApprovedAgentById(effectiveAgentId) == null) {
                throw new IllegalArgumentException("Selected agent is unavailable.");
            }

            if (!appointmentService.hasCompletedAppointment(loggedInUserId, effectiveAgentId)) {
                throw new IllegalArgumentException("You can review an agent only after a completed appointment.");
            }

            Review review = new Review();
            review.setAgentId(effectiveAgentId);
            review.setUserId(loggedInUserId);
            review.setComment(comment);
            reviewService.saveReview(review);
            redirectAttributes.addFlashAttribute("toastSuccess", "Review submitted successfully.");
            return "redirect:/agents/" + effectiveAgentId;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/reviews/new?agentId=" + safe(agentId);
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        String loggedInUserId = SessionUtils.getLoggedInUserId(session);
        if (!SessionUtils.isUser(session) || loggedInUserId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only logged-in users can edit reviews.");
            return "redirect:/login?next=/reviews/" + id + "/edit";
        }

        Review review = reviewService.getReviewById(id);
        if (review == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Review not found.");
            return "redirect:/reviews/my";
        }

        if (!safe(loggedInUserId).equals(safe(review.getUserId()))) {
            redirectAttributes.addFlashAttribute("errorMessage", "You can only edit your own review.");
            return "redirect:/reviews/my";
        }

        Agent selectedAgent = review.getAgentId() == null ? null : agentService.getAgentById(review.getAgentId());

        model.addAttribute("review", review);
        model.addAttribute("formMode", "edit");
        model.addAttribute("selectedAgentName", selectedAgent != null ? selectedAgent.getName() : "Selected Agent");
        model.addAttribute("currentUserName", session == null ? null : session.getAttribute("loggedInUserName"));
        return "review/review-form";
    }

    @GetMapping("/edit/{id}")
    public String aliasEditReview(@PathVariable String id) {
        return "redirect:/reviews/" + safe(id) + "/edit";
    }

    @PostMapping("/{id}/update")
    public String updateReview(@PathVariable String id,
            @RequestParam("agentId") String agentId,
            @RequestParam("comment") String comment,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String loggedInUserId = SessionUtils.getLoggedInUserId(session);
        if (!SessionUtils.isUser(session) || loggedInUserId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only logged-in users can update reviews.");
            return "redirect:/login?next=/reviews/" + id + "/edit";
        }

        Review existing = reviewService.getReviewById(id);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Review not found.");
            return "redirect:/reviews/my";
        }

        try {
            if (!safe(loggedInUserId).equals(safe(existing.getUserId()))) {
                redirectAttributes.addFlashAttribute("errorMessage", "You can only edit your own review.");
                return "redirect:/reviews/my";
            }

            existing.setAgentId(safe(existing.getAgentId()));
            existing.setUserId(loggedInUserId);
            existing.setComment(comment);
            reviewService.updateReviewSecure(existing, loggedInUserId, false);
            redirectAttributes.addFlashAttribute("toastSuccess", "Review updated successfully.");
            String targetAgentId = safe(agentId).isBlank() ? safe(existing.getAgentId()) : safe(agentId);
            return "redirect:/agents/" + targetAgentId;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/reviews/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteReview(@PathVariable String id,
            @RequestParam(name = "agentId", required = false) String agentId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String loggedInUserId = SessionUtils.getLoggedInUserId(session);
        if (!SessionUtils.isUser(session) && !SessionUtils.isAdmin(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to continue.");
            return "redirect:/login";
        }

        try {
            reviewService.deleteReviewSecure(id, loggedInUserId, SessionUtils.isAdmin(session));
            redirectAttributes.addFlashAttribute("toastSuccess", "Review deleted.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        if (SessionUtils.isAdmin(session) && agentId != null && !agentId.isBlank()) {
            return "redirect:/reviews?agentId=" + agentId;
        }
        return "redirect:/reviews/my";
    }

    @PostMapping("/delete/{id}")
    public String aliasDeleteReview(@PathVariable String id,
            @RequestParam(name = "agentId", required = false) String agentId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        return deleteReview(id, agentId, session, redirectAttributes);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
