package com.realestate.app.model;

import com.realestate.app.util.ValidationUtils;

/**
 * Represents a real estate agent.
 * Inherits from Person. Demonstrates Inheritance, Encapsulation.
 */
public class Agent extends Person {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    // ── Private Fields ──
    private String username;
    private String address;
    private String nicNumber;
    private String specialization; // e.g. "Residential", "Commercial"
    private String location; // City / area
    private int yearsOfExperience;
    private double averageRating; // Maintained by RatingService
    private int totalRatings; // Count for recalculating average
    private String bio; // Short profile description
    private String profileImage; // Filename in /images/agents/
    private String approvalStatus;
    private String createdAt;

    // ── Constructors ──

    public Agent() {
        super();
        this.averageRating = 0.0;
        this.totalRatings = 0;
        this.approvalStatus = STATUS_PENDING;
    }

    public Agent(String id, String name, String email, String phone, String password,
            String specialization, String location, int yearsOfExperience) {
        super(id, name, email, phone, password);
        this.specialization = specialization;
        this.location = location;
        this.yearsOfExperience = yearsOfExperience;
        this.averageRating = 0.0;
        this.totalRatings = 0;
    }

    // ── Business Method: update running average ──

    /**
     * Recalculates averageRating when a new rating is submitted.
     * Demonstrates encapsulation — the logic lives here, not in the service.
     *
     * @param newScore integer score 1–5
     */
    public void addRating(int newScore) {
        this.averageRating = ((this.averageRating * this.totalRatings) + newScore)
                / (double) (this.totalRatings + 1);
        this.totalRatings++;
        // Round to 1 decimal for cleanliness
        this.averageRating = Math.round(this.averageRating * 10.0) / 10.0;
    }

    // ── Polymorphic Overrides ──

    @Override
    public String getRole() {
        return "AGENT";
    }

    /**
     * CSV format:
     * id,name,email,phone,password,username,address,nicNumber,specialization,location,
     * yearsOfExperience,averageRating,totalRatings,bio,profileImage,approvalStatus,createdAt
     */
    @Override
    public String toFileString() {
        return csvSafe(getId()) + "," + csvSafe(getName()) + "," + csvSafe(getEmail()) + "," + csvSafe(getPhone()) + ","
            + csvSafe(getPassword()) + "," + csvSafe(username) + "," + csvSafe(address) + "," + csvSafe(nicNumber) + ","
            + csvSafe(specialization) + "," + csvSafe(location) + ","
            + yearsOfExperience + "," + averageRating + "," + totalRatings + ","
            + csvSafe(bio) + "," + csvSafe(profileImage) + ","
            + csvSafe(approvalStatus) + "," + csvSafe(createdAt);
    }

    // ── Parse from CSV line ──

    public static Agent fromFileString(String line) {
        String[] p = line.split(",", -1);
        Agent agent = new Agent();

        agent.setId(getPart(p, 0));
        agent.setName(getPart(p, 1));
        agent.setEmail(getPart(p, 2));
        agent.setPhone(getPart(p, 3));
        agent.setPassword(getPart(p, 4));

        // Backward compatibility for old format.
        if (p.length >= 17) {
            agent.setUsername(blankToNull(getPart(p, 5)));
            agent.setAddress(blankToNull(getPart(p, 6)));
            agent.setNicNumber(blankToNull(getPart(p, 7)));
            agent.setSpecialization(blankToNull(getPart(p, 8)));
            agent.setLocation(blankToNull(getPart(p, 9)));
            agent.setYearsOfExperience(parseIntSafe(getPart(p, 10)));
            agent.setAverageRating(parseDoubleSafe(getPart(p, 11)));
            agent.setTotalRatings(parseIntSafe(getPart(p, 12)));
            agent.setBio(blankToNull(getPart(p, 13)));
            agent.setProfileImage(blankToNull(getPart(p, 14)));
            agent.setApprovalStatus(normalizeStatus(getPart(p, 15)));
            agent.setCreatedAt(blankToNull(getPart(p, 16)));
        } else {
            agent.setSpecialization(blankToNull(getPart(p, 5)));
            agent.setLocation(blankToNull(getPart(p, 6)));
            agent.setYearsOfExperience(parseIntSafe(getPart(p, 7)));
            agent.setAverageRating(parseDoubleSafe(getPart(p, 8)));
            agent.setTotalRatings(parseIntSafe(getPart(p, 9)));
            agent.setBio(blankToNull(getPart(p, 10)));
            agent.setProfileImage(blankToNull(getPart(p, 11)));
            agent.setApprovalStatus(STATUS_APPROVED);
        }

        if (agent.getUsername() == null || agent.getUsername().isBlank()) {
            String fallback = ValidationUtils.normalize(agent.getName());
            agent.setUsername(fallback == null ? null : fallback.toLowerCase().replaceAll("\\s+", ""));
        }

        return agent;
    }

    private static String getPart(String[] parts, int index) {
        return parts.length > index ? parts[index] : "";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_PENDING;
        }
        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case STATUS_PENDING, STATUS_APPROVED, STATUS_REJECTED -> normalized;
            default -> STATUS_PENDING;
        };
    }

    // ── Private helper ──

    private String csvSafe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(',', ' ').replace('\n', ' ').replace('\r', ' ').trim();
    }

    // ── Getters ──

    public String getSpecialization() {
        return specialization;
    }

    public String getUsername() {
        return username;
    }

    public String getAddress() {
        return address;
    }

    public String getNicNumber() {
        return nicNumber;
    }

    public String getLocation() {
        return location;
    }

    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public int getTotalRatings() {
        return totalRatings;
    }

    public String getBio() {
        return bio;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public boolean isApproved() {
        return STATUS_APPROVED.equals(approvalStatus);
    }

    // ── Setters ──

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setNicNumber(String nicNumber) {
        this.nicNumber = nicNumber;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setYearsOfExperience(int yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public void setTotalRatings(int totalRatings) {
        this.totalRatings = totalRatings;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = normalizeStatus(approvalStatus);
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
