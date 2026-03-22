package com.realestate.app.controller;

import com.realestate.app.auth.SessionUtils;
import com.realestate.app.model.Agent;
import com.realestate.app.service.AgentService;
import com.realestate.app.service.AppointmentService;
import com.realestate.app.service.RatingService;
import com.realestate.app.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AgentDashboardController {
    private final AgentService agentService;
    private final AppointmentService appointmentService;
    private final ReviewService reviewService;
    private final RatingService ratingService;

    public AgentDashboardController(AgentService agentService,
            AppointmentService appointmentService,
            ReviewService reviewService,
            RatingService ratingService) {
        this.agentService = agentService;
        this.appointmentService = appointmentService;
        this.reviewService = reviewService;
        this.ratingService = ratingService;
    }

    @GetMapping("/agent/dashboard")
    public String dashboard(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        String loggedInAgentId = SessionUtils.getLoggedInUserId(session);
        if (!SessionUtils.isAgent(session) || loggedInAgentId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login as an agent to access the dashboard.");
            return "redirect:/login?next=/agent/dashboard";
        }

        Agent agent = agentService.getAgentById(loggedInAgentId);
        if (agent == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Agent profile not found.");
            return "redirect:/agents";
        }

        model.addAttribute("agent", agent);
        model.addAttribute("appointments", appointmentService.getAppointmentsByAgent(loggedInAgentId));
        model.addAttribute("reviews", reviewService.getReviewsByAgent(loggedInAgentId));
        model.addAttribute("ratings", ratingService.getRatingsByAgent(loggedInAgentId));
        model.addAttribute("averageRating", ratingService.calculateAverageRating(loggedInAgentId));
        return "agent/dashboard";
    }

    @GetMapping("/agent/reviews")
    public String agentReviews(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        String loggedInAgentId = SessionUtils.getLoggedInUserId(session);
        if (!SessionUtils.isAgent(session) || loggedInAgentId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login as an agent to view your reviews.");
            return "redirect:/login?next=/agent/reviews";
        }

        model.addAttribute("reviews", reviewService.getReviewsByAgent(loggedInAgentId));
        model.addAttribute("agentId", loggedInAgentId);
        model.addAttribute("pageTitle", "Reviews About Me");
        return "agent/agent-reviews";
    }

    @GetMapping("/agent/ratings")
    public String agentRatings(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        String loggedInAgentId = SessionUtils.getLoggedInUserId(session);
        if (!SessionUtils.isAgent(session) || loggedInAgentId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login as an agent to view your ratings.");
            return "redirect:/login?next=/agent/ratings";
        }

        model.addAttribute("ratings", ratingService.getRatingsByAgent(loggedInAgentId));
        model.addAttribute("averageRating", ratingService.calculateAverageRating(loggedInAgentId));
        model.addAttribute("agentId", loggedInAgentId);
        model.addAttribute("pageTitle", "Ratings About Me");
        return "agent/agent-ratings";
    }

    @GetMapping("/agent/appointments")
    public String agentAppointmentsRedirect() {
        return "redirect:/appointments/agent";
    }
}
