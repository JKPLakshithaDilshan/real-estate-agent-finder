package com.realestate.app.controller;

import com.realestate.app.auth.SessionUtils;
import com.realestate.app.dto.AppointmentForm;
import com.realestate.app.model.Agent;
import com.realestate.app.model.Appointment;
import com.realestate.app.service.AgentService;
import com.realestate.app.service.AppointmentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;
    private final AgentService agentService;

    public AppointmentController(AppointmentService appointmentService, AgentService agentService) {
        this.appointmentService = appointmentService;
        this.agentService = agentService;
    }

    @GetMapping
    public String listAppointments(
            @RequestParam(name = "agentId", required = false) String agentId,
            @RequestParam(name = "userId", required = false) String userId,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (SessionUtils.isUser(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Use My Appointments to view your own bookings.");
            return "redirect:/appointments/my";
        }

        if (SessionUtils.isAgent(session)) {
            return "redirect:/appointments/agent";
        }

        if (!SessionUtils.isAdmin(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login as admin to access all appointments.");
            return "redirect:/admin/login";
        }

        List<Appointment> appointments = appointmentService.searchAppointments(agentId, userId, date);
        model.addAttribute("appointments", appointments);
        model.addAttribute("agentId", safe(agentId));
        model.addAttribute("userId", safe(userId));
        model.addAttribute("date", date);
        model.addAttribute("adminView", true);
        model.addAttribute("statuses", List.of(
                Appointment.STATUS_PENDING,
                Appointment.STATUS_CONFIRMED,
                Appointment.STATUS_CANCELLED,
                Appointment.STATUS_COMPLETED));
        return "appointment/appointment-list";
    }

    @GetMapping("/my")
    public String myAppointments(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        String loggedInUserId = SessionUtils.getLoggedInUserId(session);
        if (!SessionUtils.isUser(session) || loggedInUserId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to view your appointments.");
            return "redirect:/login?next=/appointments/my";
        }

        List<Appointment> appointments = appointmentService.getAppointmentsByUser(loggedInUserId).stream()
                .sorted((a, b) -> {
                    if (a.getAppointmentDateTime() == null && b.getAppointmentDateTime() == null) {
                        return 0;
                    }
                    if (a.getAppointmentDateTime() == null) {
                        return 1;
                    }
                    if (b.getAppointmentDateTime() == null) {
                        return -1;
                    }
                    return b.getAppointmentDateTime().compareTo(a.getAppointmentDateTime());
                })
                .collect(Collectors.toList());

        model.addAttribute("appointments", appointments);
        return "appointment/my-appointments";
    }

    @GetMapping("/agent")
    public String agentAppointments(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        String loggedInAgentId = SessionUtils.getLoggedInUserId(session);
        if (!SessionUtils.isAgent(session) || loggedInAgentId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login as an agent to view your appointments.");
            return "redirect:/login?next=/appointments/agent";
        }

        List<Appointment> appointments = appointmentService.getAppointmentsByAgent(loggedInAgentId).stream()
                .sorted((a, b) -> {
                    if (a.getAppointmentDateTime() == null && b.getAppointmentDateTime() == null) {
                        return 0;
                    }
                    if (a.getAppointmentDateTime() == null) {
                        return 1;
                    }
                    if (b.getAppointmentDateTime() == null) {
                        return -1;
                    }
                    return b.getAppointmentDateTime().compareTo(a.getAppointmentDateTime());
                })
                .collect(Collectors.toList());

        model.addAttribute("appointments", appointments);
        model.addAttribute("statuses", List.of(
                Appointment.STATUS_PENDING,
                Appointment.STATUS_CONFIRMED,
                Appointment.STATUS_CANCELLED,
                Appointment.STATUS_COMPLETED));
        return "appointment/agent-appointments";
    }

    @GetMapping("/new")
    public String showCreateForm(
            @RequestParam(name = "agentId", required = false) String agentId,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String loggedInUserId = SessionUtils.getLoggedInUserId(session);
        if (!SessionUtils.isUser(session) || loggedInUserId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login as a user to book appointments.");
            return "redirect:/login?next=/appointments/new?agentId=" + safe(agentId);
        }

        AppointmentForm form = new AppointmentForm();
        String selectedAgentId = safe(agentId);
        form.setAgentId(selectedAgentId);
        form.setUserId(loggedInUserId);

        Agent selectedAgent = selectedAgentId.isBlank() ? null : agentService.getApprovedAgentById(selectedAgentId);
        if (selectedAgent == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Selected agent is unavailable.");
            return "redirect:/agents";
        }

        model.addAttribute("appointmentForm", form);
        model.addAttribute("selectedAgentName", selectedAgent != null ? selectedAgent.getName() : "Selected Agent");
        model.addAttribute("currentUserName", session == null ? null : session.getAttribute("loggedInUserName"));
        return "appointment/appointment-form";
    }

    @PostMapping("/save")
    public String createAppointment(@ModelAttribute("appointmentForm") AppointmentForm form,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            String loggedInUserId = SessionUtils.getLoggedInUserId(session);
            if (!SessionUtils.isUser(session) || loggedInUserId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Only logged-in users can book appointments.");
                return "redirect:/login?next=/appointments/new?agentId=" + safe(form.getAgentId());
            }

            String effectiveAgentId = safe(form.getAgentId());
            if (effectiveAgentId.isBlank()) {
                throw new IllegalArgumentException("Please select an agent before booking an appointment.");
            }

            if (agentService.getApprovedAgentById(effectiveAgentId) == null) {
                throw new IllegalArgumentException("Selected agent is unavailable.");
            }

            Appointment appointment = new Appointment();
            appointment.setAgentId(effectiveAgentId);
            appointment.setUserId(loggedInUserId);
            appointment.setAppointmentDateTime(form.getAppointmentDateTime());
            appointment.setNotes(form.getNotes());
            appointment.setStatus(Appointment.STATUS_PENDING);

            appointmentService.saveAppointment(appointment);
            redirectAttributes.addFlashAttribute("toastSuccess", "Appointment booked successfully.");
            return "redirect:/appointments/my";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            String fallbackAgentId = safe(form.getAgentId());
            if (!fallbackAgentId.isBlank()) {
                return "redirect:/appointments/new?agentId=" + fallbackAgentId;
            }
            return "redirect:/appointments/new";
        }
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable String id,
            @RequestParam("status") String status,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        Appointment appointment = appointmentService.getAppointmentById(id);
        if (appointment == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Appointment not found.");
            return "redirect:/appointments";
        }

        boolean isAdmin = SessionUtils.isAdmin(session);
        boolean isAssignedAgent = SessionUtils.isAgent(session)
                && SessionUtils.getLoggedInUserId(session) != null
                && SessionUtils.getLoggedInUserId(session).equals(appointment.getAgentId());

        if (!isAdmin && !isAssignedAgent) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only admin or the assigned agent can update appointment status.");
            return "redirect:/appointments/my";
        }

        try {
            appointmentService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("toastSuccess", "Appointment status updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        if (isAssignedAgent) {
            return "redirect:/appointments/agent";
        }
        return "redirect:/appointments";
    }

    @GetMapping("/{id}/reschedule")
    public String showRescheduleForm(@PathVariable String id,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        Appointment appointment = appointmentService.getAppointmentById(id);
        if (appointment == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Appointment not found.");
            return SessionUtils.isAdmin(session) ? "redirect:/appointments" : "redirect:/appointments/my";
        }

        if (!canAccessAppointmentForViewing(appointment, session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot access this appointment.");
            return SessionUtils.isAdmin(session) ? "redirect:/appointments" : "redirect:/appointments/my";
        }

        if (SessionUtils.isAgent(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Agents can update status but cannot reschedule client appointments.");
            return "redirect:/appointments/agent";
        }

        if (Appointment.STATUS_COMPLETED.equals(appointment.getStatus())
                || Appointment.STATUS_CANCELLED.equals(appointment.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Completed or cancelled appointments cannot be rescheduled.");
            return SessionUtils.isAdmin(session) ? "redirect:/appointments" : "redirect:/appointments/my";
        }

        AppointmentForm form = new AppointmentForm();
        form.setAgentId(appointment.getAgentId());
        form.setUserId(appointment.getUserId());
        form.setAppointmentDateTime(appointment.getAppointmentDateTime());
        form.setNotes(appointment.getNotes());
        form.setStatus(appointment.getStatus());

        model.addAttribute("appointment", appointment);
        model.addAttribute("appointmentForm", form);
        Agent selectedAgent = appointment.getAgentId() == null ? null : agentService.getAgentById(appointment.getAgentId());
        model.addAttribute("selectedAgentName", selectedAgent != null ? selectedAgent.getName() : "Selected Agent");
        model.addAttribute("currentUserName", session == null ? null : session.getAttribute("loggedInUserName"));
        model.addAttribute("statuses", List.of(
            Appointment.STATUS_PENDING,
            Appointment.STATUS_CONFIRMED,
            Appointment.STATUS_CANCELLED,
            Appointment.STATUS_COMPLETED));
        return "appointment/reschedule-form";
    }

    @GetMapping("/edit/{id}")
    public String aliasEditAppointment(@PathVariable String id) {
        return "redirect:/appointments/" + safe(id) + "/reschedule";
    }

    @GetMapping("/{id}/edit")
    public String aliasEditAppointmentAlt(@PathVariable String id) {
        return "redirect:/appointments/" + safe(id) + "/reschedule";
    }

    @PostMapping("/{id}/reschedule")
    public String reschedule(@PathVariable String id,
            @ModelAttribute("appointmentForm") AppointmentForm form,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        Appointment existing = appointmentService.getAppointmentById(id);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Appointment not found.");
            return SessionUtils.isAdmin(session) ? "redirect:/appointments" : "redirect:/appointments/my";
        }

        if (!canAccessAppointmentForViewing(existing, session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot modify this appointment.");
            return SessionUtils.isAdmin(session) ? "redirect:/appointments" : "redirect:/appointments/my";
        }

        if (SessionUtils.isAgent(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Agents can update status but cannot reschedule client appointments.");
            return "redirect:/appointments/agent";
        }

        if (Appointment.STATUS_COMPLETED.equals(existing.getStatus())
                || Appointment.STATUS_CANCELLED.equals(existing.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Completed or cancelled appointments cannot be rescheduled.");
            return SessionUtils.isAdmin(session) ? "redirect:/appointments" : "redirect:/appointments/my";
        }

        try {
            appointmentService.rescheduleSecure(id, form.getAppointmentDateTime(), form.getNotes(), SessionUtils.getLoggedInUserId(session), SessionUtils.isAdmin(session));
            if (SessionUtils.isAdmin(session) && form.getStatus() != null && !form.getStatus().isBlank()) {
                appointmentService.updateStatus(id, form.getStatus());
            }
            redirectAttributes.addFlashAttribute("toastSuccess", "Appointment rescheduled successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return SessionUtils.isAdmin(session) ? "redirect:/appointments" : "redirect:/appointments/my";
    }

    @PostMapping("/{id}/update")
    public String aliasUpdateAppointment(@PathVariable String id,
            @ModelAttribute("appointmentForm") AppointmentForm form,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        return reschedule(id, form, redirectAttributes, session);
    }

    @PostMapping("/update/{id}")
    public String aliasUpdateAppointmentAlt(@PathVariable String id,
            @ModelAttribute("appointmentForm") AppointmentForm form,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        return reschedule(id, form, redirectAttributes, session);
    }

    @PostMapping("/{id}/cancel")
    public String cancelAppointment(@PathVariable String id,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        Appointment appointment = appointmentService.getAppointmentById(id);
        if (appointment == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Appointment not found.");
            return "redirect:/appointments/my";
        }

        String loggedInUserId = SessionUtils.getLoggedInUserId(session);
        if (!SessionUtils.isAdmin(session) && (loggedInUserId == null || !loggedInUserId.equals(appointment.getUserId()))) {
            redirectAttributes.addFlashAttribute("errorMessage", "You can only cancel your own appointments.");
            return "redirect:/appointments/my";
        }

        try {
            appointmentService.cancelForActor(id, loggedInUserId, SessionUtils.isAdmin(session));
            redirectAttributes.addFlashAttribute("toastSuccess", "Appointment cancelled.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return SessionUtils.isAdmin(session) ? "redirect:/appointments" : "redirect:/appointments/my";
    }

    @PostMapping("/{id}/delete")
    public String deleteCancelledOrCompleted(@PathVariable String id,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        Appointment existing = appointmentService.getAppointmentById(id);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Appointment not found.");
            return SessionUtils.isAdmin(session) ? "redirect:/appointments" : "redirect:/appointments/my";
        }

        boolean isAdmin = SessionUtils.isAdmin(session);
        boolean isOwnerUser = SessionUtils.isUser(session)
                && SessionUtils.getLoggedInUserId(session) != null
                && SessionUtils.getLoggedInUserId(session).equals(existing.getUserId());

        if (!isAdmin && !isOwnerUser) {
            redirectAttributes.addFlashAttribute("errorMessage", "You can only delete your own completed/cancelled appointments.");
            return "redirect:/appointments/my";
        }

        try {
            appointmentService.deleteIfCancelledOrCompleted(id);
            redirectAttributes.addFlashAttribute("toastSuccess", "Appointment deleted.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return isAdmin ? "redirect:/appointments" : "redirect:/appointments/my";
    }

    @PostMapping("/delete/{id}")
    public String aliasDeleteAppointment(@PathVariable String id,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        return deleteCancelledOrCompleted(id, redirectAttributes, session);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean canAccessAppointmentForViewing(Appointment appointment, HttpSession session) {
        if (SessionUtils.isAdmin(session)) {
            return true;
        }

        String loggedInId = SessionUtils.getLoggedInUserId(session);
        if (loggedInId == null) {
            return false;
        }

        if (SessionUtils.isUser(session)) {
            return loggedInId.equals(appointment.getUserId());
        }

        if (SessionUtils.isAgent(session)) {
            return loggedInId.equals(appointment.getAgentId());
        }

        return false;
    }
}
