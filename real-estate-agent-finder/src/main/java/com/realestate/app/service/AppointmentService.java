package com.realestate.app.service;

import com.realestate.app.model.Appointment;
import com.realestate.app.model.Agent;
import com.realestate.app.model.User;
import com.realestate.app.util.FileHandler;
import com.realestate.app.util.ValidationUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentService {
    private static final String FILE_PATH = "src/main/resources/data/appointments.txt";

    public List<Appointment> getAllAppointments() {
        return FileHandler.readAppointments(FILE_PATH);
    }

    public Appointment getAppointmentById(String id) {
        return getAllAppointments().stream()
                .filter(a -> a.getId().equals(id))
                .findFirst().orElse(null);
    }

    public List<Appointment> getAppointmentsByUser(String userId) {
        return getAllAppointments().stream()
                .filter(a -> a.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<Appointment> getAppointmentsByAgent(String agentId) {
        return getAllAppointments().stream()
                .filter(a -> a.getAgentId().equals(agentId))
                .collect(Collectors.toList());
    }

    public List<Appointment> getAppointmentsByAgentAndUser(String agentId, String userId) {
        return getAllAppointments().stream()
                .filter(a -> equalsIgnoreCase(a.getAgentId(), agentId))
                .filter(a -> equalsIgnoreCase(a.getUserId(), userId))
                .collect(Collectors.toList());
    }

    public boolean hasCompletedAppointment(String userId, String agentId) {
        return getAllAppointments().stream()
                .anyMatch(a -> equalsIgnoreCase(a.getUserId(), userId)
                        && equalsIgnoreCase(a.getAgentId(), agentId)
                        && Appointment.STATUS_COMPLETED.equals(a.getStatus()));
    }

    public List<Appointment> getAppointmentsByDate(LocalDate date) {
        return getAllAppointments().stream()
                .filter(a -> a.getAppointmentDateTime() != null
                        && a.getAppointmentDateTime().toLocalDate().equals(date))
                .collect(Collectors.toList());
    }

    public List<Appointment> searchAppointments(String agentId, String userId, LocalDate date) {
        return getAllAppointments().stream()
                .filter(a -> isBlank(agentId) || equalsIgnoreCase(a.getAgentId(), agentId))
                .filter(a -> isBlank(userId) || equalsIgnoreCase(a.getUserId(), userId))
                .filter(a -> date == null || (a.getAppointmentDateTime() != null
                        && a.getAppointmentDateTime().toLocalDate().equals(date)))
                .collect(Collectors.toList());
    }

    private String generateNextAppointmentId(List<Appointment> existing) {
        int maxId = existing.stream()
                .map(Appointment::getId)
                .filter(id -> id != null)
                .mapToInt(id -> {
                    try {
                        return Integer.parseInt(id.replaceAll("[^0-9]", ""));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
        return String.format("AP%03d", maxId + 1);
    }

    public void saveAppointment(Appointment appointment) {
        validateAppointment(appointment, false);
        List<Appointment> appointments = getAllAppointments();
        appointment.setId(generateNextAppointmentId(appointments));
        if (appointment.getStatus() == null || appointment.getStatus().isBlank()) {
            appointment.setStatus(Appointment.STATUS_PENDING);
        }
        if (appointment.getCreatedAt() == null) {
            appointment.setCreatedAt(LocalDateTime.now());
        }
        appointments.add(appointment);
        FileHandler.writeAppointments(FILE_PATH, appointments);
    }

    public void updateAppointment(Appointment updated) {
        validateAppointment(updated, true);
        List<Appointment> appointments = getAllAppointments();
        appointments.replaceAll(a -> a.getId().equals(updated.getId()) ? updated : a);
        FileHandler.writeAppointments(FILE_PATH, appointments);
    }

    public void deleteAppointment(String id) {
        List<Appointment> appointments = getAllAppointments();
        appointments.removeIf(a -> a.getId().equals(id));
        FileHandler.writeAppointments(FILE_PATH, appointments);
    }

    public void updateStatus(String id, String status) {
        Appointment appointment = getAppointmentById(id);
        if (appointment == null) {
            throw new IllegalArgumentException("Appointment not found.");
        }

        String normalizedStatus = normalizeStatus(status);
        appointment.setStatus(normalizedStatus);
        updateAppointment(appointment);
    }

    public void reschedule(String id, LocalDateTime newDateTime, String notes) {
        Appointment appointment = getAppointmentById(id);
        if (appointment == null) {
            throw new IllegalArgumentException("Appointment not found.");
        }

        ValidationUtils.validateAppointmentDateTime(newDateTime, false);
        appointment.setAppointmentDateTime(newDateTime);
        appointment.setNotes(ValidationUtils.normalize(notes));
        if (Appointment.STATUS_CANCELLED.equals(appointment.getStatus())
                || Appointment.STATUS_COMPLETED.equals(appointment.getStatus())) {
            appointment.setStatus(Appointment.STATUS_PENDING);
        }
        updateAppointment(appointment);
    }

    public void rescheduleSecure(String id, LocalDateTime newDateTime, String notes, String actorUserId, boolean isAdmin) {
        Appointment appointment = getAppointmentById(id);
        if (appointment == null) {
            throw new IllegalArgumentException("Appointment not found.");
        }
        if (!isAdmin) {
            if (actorUserId == null || actorUserId.isBlank()) {
                throw new IllegalArgumentException("Please login to modify appointments.");
            }
            if (!actorUserId.equals(appointment.getUserId())) {
                throw new IllegalArgumentException("You can only modify your own appointments.");
            }
        }
        if (Appointment.STATUS_COMPLETED.equals(appointment.getStatus())
                || Appointment.STATUS_CANCELLED.equals(appointment.getStatus())) {
            throw new IllegalArgumentException("Completed or cancelled appointments cannot be rescheduled.");
        }

        reschedule(id, newDateTime, notes);
    }

    public void cancelForActor(String id, String actorUserId, boolean isAdmin) {
        Appointment appointment = getAppointmentById(id);
        if (appointment == null) {
            throw new IllegalArgumentException("Appointment not found.");
        }
        if (!isAdmin) {
            if (actorUserId == null || actorUserId.isBlank()) {
                throw new IllegalArgumentException("Please login to cancel appointments.");
            }
            if (!actorUserId.equals(appointment.getUserId())) {
                throw new IllegalArgumentException("You can only cancel your own appointments.");
            }
        }

        boolean canCancel = Appointment.STATUS_PENDING.equals(appointment.getStatus())
                || Appointment.STATUS_CONFIRMED.equals(appointment.getStatus());
        if (!canCancel) {
            throw new IllegalArgumentException("Only pending or confirmed appointments can be cancelled.");
        }

        updateStatus(id, Appointment.STATUS_CANCELLED);
    }

    public void deleteIfCancelledOrCompleted(String id) {
        Appointment appointment = getAppointmentById(id);
        if (appointment == null) {
            throw new IllegalArgumentException("Appointment not found.");
        }

        boolean canDelete = Appointment.STATUS_CANCELLED.equals(appointment.getStatus())
                || Appointment.STATUS_COMPLETED.equals(appointment.getStatus());
        if (!canDelete) {
            throw new IllegalArgumentException("Only cancelled or completed appointments can be deleted.");
        }

        deleteAppointment(id);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required.");
        }

        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case Appointment.STATUS_PENDING,
                    Appointment.STATUS_CONFIRMED,
                    Appointment.STATUS_CANCELLED,
                    Appointment.STATUS_COMPLETED -> normalized;
            default -> throw new IllegalArgumentException("Invalid appointment status: " + status);
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right.trim());
    }

    private void validateAppointment(Appointment appointment, boolean allowPast) {
        appointment.setAgentId(ValidationUtils.normalizeRequired(appointment.getAgentId(), "Agent ID"));
        appointment.setUserId(ValidationUtils.normalizeRequired(appointment.getUserId(), "User ID"));
        appointment.setNotes(ValidationUtils.normalize(appointment.getNotes()));
        if (!agentExists(appointment.getAgentId())) {
            throw new IllegalArgumentException("Selected agent does not exist.");
        }
        if (!userExists(appointment.getUserId())) {
            throw new IllegalArgumentException("Selected user does not exist.");
        }
        ValidationUtils.validateAppointmentDateTime(appointment.getAppointmentDateTime(), allowPast);
    }

    private boolean agentExists(String agentId) {
        return FileHandler.readAgents("src/main/resources/data/agents.txt").stream()
                .filter(agent -> Agent.STATUS_APPROVED.equals(agent.getApprovalStatus()))
                .map(Agent::getId)
                .anyMatch(agentId::equals);
    }

    private boolean userExists(String userId) {
        return FileHandler.readUsers("src/main/resources/data/users.txt").stream()
                .map(User::getId)
                .anyMatch(userId::equals);
    }
}
