package com.realestate.app.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a numeric rating (1–5 stars) given by a User to an Agent.
 * Demonstrates: Encapsulation, validation in setter.
 */
public class Rating {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    // ── Private Fields ──
    private String id;
    private String userId;
    private String agentId;
    private int score; // Must be 1–5
    private LocalDateTime ratedAt;

    // ── Constructors ──

    public Rating() {
        this.ratedAt = LocalDateTime.now();
    }

    public Rating(String id, String userId, String agentId, int score) {
        this.id = id;
        this.userId = userId;
        this.agentId = agentId;
        setScore(score); // Validate via setter
        this.ratedAt = LocalDateTime.now();
    }

    // ── File Persistence ──

    /**
     * CSV format: id,userId,agentId,score,ratedAt
     */
    public String toFileString() {
        return id + "," + userId + "," + agentId + "," + score + ","
                + (ratedAt != null ? ratedAt.format(FORMATTER) : "");
    }

    public static Rating fromFileString(String line) {
        String[] p = line.split(",", -1);
        Rating rating = new Rating();
        rating.setId(p[0]);
        rating.setUserId(p[1]);
        rating.setAgentId(p[2]);
        rating.setScore(Integer.parseInt(p[3]));
        if (p.length > 4 && !p[4].isBlank()) {
            rating.setRatedAt(LocalDateTime.parse(p[4], FORMATTER));
        }
        return rating;
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

    public int getScore() {
        return score;
    }

    public LocalDateTime getRatedAt() {
        return ratedAt;
    }

    // ── Setters (with validation on score) ──

    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    /**
     * Validates that score is between 1 and 5 inclusive.
     * Demonstrates information hiding — invalid data cannot enter the object.
     */
    public void setScore(int score) {
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("Rating score must be between 1 and 5. Got: " + score);
        }
        this.score = score;
    }

    public void setRatedAt(LocalDateTime ratedAt) {
        this.ratedAt = ratedAt;
    }

    @Override
    public String toString() {
        return "Rating[id=" + id + ", agent=" + agentId + ", user=" + userId
                + ", score=" + score + "]";
    }
}
