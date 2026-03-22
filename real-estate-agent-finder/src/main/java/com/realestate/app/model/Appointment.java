package com.realestate.app.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a scheduled meeting between a User and an Agent.
 * Standalone entity — does not extend Person.
 * Demonstrates: Encapsulation, clean domain modelling.
 */
public class Appointment {

    // ── Status Enum-like constants ──
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    // ── Private Fields ──
    private String id;
    private String userId;
    private String agentId;
    private LocalDateTime appointmentDateTime;
    private String status;
    private String notes;
    private LocalDateTime createdAt;

    // ── Constructors ──

    public Appointment() {
        this.status = STATUS_PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public Appointment(String id, String userId, String agentId,
            LocalDateTime appointmentDateTime, String status, String notes) {
        this.id = id;
        this.userId = userId;
        this.agentId = agentId;
        this.appointmentDateTime = appointmentDateTime;
        this.status = status;
        this.notes = notes;
        this.createdAt = LocalDateTime.now();
    }

    // ── Business Methods ──

    /** Returns true if this appointment can still be modified. */
    public boolean isMutable() {
        return STATUS_PENDING.equals(status) || STATUS_CONFIRMED.equals(status);
    }

    /** Returns true if the appointment date is in the future. */
    public boolean isUpcoming() {
        return appointmentDateTime != null && appointmentDateTime.isAfter(LocalDateTime.now());
    }

    // ── File Persistence ──

    /**
     * CSV format: id,userId,agentId,dateTime,status,notes,createdAt
     */
    public String toFileString() {
        return id + "," + userId + "," + agentId + ","
                + (appointmentDateTime != null ? appointmentDateTime.format(FORMATTER) : "") + ","
                + status + "," + nullSafe(notes) + ","
                + (createdAt != null ? createdAt.format(FORMATTER) : "");
    }

    public static Appointment fromFileString(String line) {
        String[] p = line.split(",", -1);
        Appointment apt = new Appointment();
        apt.setId(p[0]);
        apt.setUserId(p[1]);
        apt.setAgentId(p[2]);
        apt.setAppointmentDateTime(p[3].isBlank() ? null : LocalDateTime.parse(p[3], FORMATTER));
        apt.setStatus(p[4]);
        apt.setNotes(p.length > 5 && !p[5].isBlank() ? p[5] : null);
        apt.setCreatedAt(p.length > 6 && !p[6].isBlank() ? LocalDateTime.parse(p[6], FORMATTER) : null);
        return apt;
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    // ── Getters ──

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getAgentId() {
        return agentId;
    }

    public LocalDateTime getAppointmentDateTime() {
        return appointmentDateTime;
    }

    public String getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ── Setters ──

    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public void setAppointmentDateTime(LocalDateTime dt) {
        this.appointmentDateTime = dt;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Appointment[id=" + id + ", user=" + userId + ", agent=" + agentId
                + ", time=" + appointmentDateTime + ", status=" + status + "]";
    }
}
