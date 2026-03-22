package com.realestate.app.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a written review left by a User for an Agent.
 * Demonstrates: Encapsulation, clean domain modelling.
 */
public class Review {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    // Max characters allowed in a review comment
    public static final int MAX_COMMENT_LENGTH = 1000;

    // ── Private Fields ──
    private String id;
    private String userId;
    private String agentId;
    private String comment;
    private LocalDateTime createdAt;
    private boolean flagged; // Admin can flag inappropriate reviews

    // ── Constructors ──

    public Review() {
        this.createdAt = LocalDateTime.now();
        this.flagged = false;
    }

    public Review(String id, String userId, String agentId, String comment) {
        this.id = id;
        this.userId = userId;
        this.agentId = agentId;
        setComment(comment); // Validate via setter
        this.createdAt = LocalDateTime.now();
        this.flagged = false;
    }

    // ── File Persistence ──

    /**
     * CSV format: id,userId,agentId,comment,createdAt,flagged
     * Note: comments with commas are escaped as [COMMA]
     */
    public String toFileString() {
        String safeComment = comment == null ? "" : comment.replace(",", "[COMMA]");
        return id + "," + userId + "," + agentId + "," + safeComment + ","
                + (createdAt != null ? createdAt.format(FORMATTER) : "") + "," + flagged;
    }

    public static Review fromFileString(String line) {
        String[] p = line.split(",", -1);
        Review review = new Review();
        review.setId(p[0]);
        review.setUserId(p[1]);
        review.setAgentId(p[2]);
        // Restore escaped commas
        review.setComment(p[3].replace("[COMMA]", ","));
        if (p.length > 4 && !p[4].isBlank()) {
            review.setCreatedAt(LocalDateTime.parse(p[4], FORMATTER));
        }
        if (p.length > 5) {
            review.setFlagged(Boolean.parseBoolean(p[5]));
        }
        return review;
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

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isFlagged() {
        return flagged;
    }

    // ── Setters (with validation on comment) ──

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
     * Validates comment is not null/blank and within max length.
     * Demonstrates information hiding / encapsulation.
     */
    public void setComment(String comment) {
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Review comment cannot be empty.");
        }
        if (comment.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException(
                    "Review comment exceeds maximum length of " + MAX_COMMENT_LENGTH + " characters.");
        }
        this.comment = comment.trim();
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    @Override
    public String toString() {
        return "Review[id=" + id + ", agent=" + agentId + ", user=" + userId
                + ", flagged=" + flagged + "]";
    }
}
