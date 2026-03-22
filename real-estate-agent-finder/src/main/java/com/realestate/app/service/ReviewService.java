package com.realestate.app.service;

import com.realestate.app.model.Review;
import com.realestate.app.model.Agent;
import com.realestate.app.model.User;
import com.realestate.app.util.FileHandler;
import com.realestate.app.util.ValidationUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {
    private static final String FILE_PATH = "src/main/resources/data/reviews.txt";

    public List<Review> getAllReviews() {
        return FileHandler.readReviews(FILE_PATH);
    }

    public List<Review> getReviewsByAgent(String agentId) {
        return getAllReviews().stream()
                .filter(r -> r.getAgentId().equals(agentId))
                .collect(Collectors.toList());
    }

    public List<Review> getReviewsByUser(String userId) {
        return getAllReviews().stream()
                .filter(r -> r.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<Review> searchReviews(String agentId, String userId) {
        return getAllReviews().stream()
                .filter(r -> isBlank(agentId) || equalsIgnoreCase(r.getAgentId(), agentId))
                .filter(r -> isBlank(userId) || equalsIgnoreCase(r.getUserId(), userId))
                .collect(Collectors.toList());
    }

    public Review getReviewById(String id) {
        return getAllReviews().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst().orElse(null);
    }

    private String generateNextReviewId(List<Review> existing) {
        int maxId = existing.stream()
                .map(Review::getId)
                .filter(value -> value != null)
                .mapToInt(value -> {
                    try {
                        return Integer.parseInt(value.replaceAll("[^0-9]", ""));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
        return String.format("RV%03d", maxId + 1);
    }

    public void saveReview(Review review) {
        validateReview(review);
        List<Review> reviews = getAllReviews();

        boolean duplicateForAgent = reviews.stream().anyMatch(existing -> existing.getUserId() != null
                && existing.getAgentId() != null
                && existing.getUserId().equalsIgnoreCase(review.getUserId())
                && existing.getAgentId().equalsIgnoreCase(review.getAgentId()));
        if (duplicateForAgent) {
            throw new IllegalArgumentException("You already reviewed this agent. Please edit your existing review.");
        }

        review.setId(generateNextReviewId(reviews));
        reviews.add(review);
        FileHandler.writeReviews(FILE_PATH, reviews);
    }

    public void updateReview(Review updated) {
        validateReview(updated);
        List<Review> reviews = getAllReviews();
        reviews.replaceAll(r -> r.getId().equals(updated.getId()) ? updated : r);
        FileHandler.writeReviews(FILE_PATH, reviews);
    }

    public void updateReviewSecure(Review updated, String actorUserId, boolean isAdmin) {
        Review existing = getReviewById(updated.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Review not found.");
        }

        if (!isAdmin) {
            String normalizedActor = ValidationUtils.normalizeRequired(actorUserId, "User");
            if (!normalizedActor.equals(existing.getUserId())) {
                throw new IllegalArgumentException("You can only update your own review.");
            }
            updated.setUserId(existing.getUserId());
            updated.setAgentId(existing.getAgentId());
        }

        updateReview(updated);
    }

    public void deleteReview(String id) {
        List<Review> reviews = getAllReviews();
        reviews.removeIf(r -> r.getId().equals(id));
        FileHandler.writeReviews(FILE_PATH, reviews);
    }

    public void deleteReviewSecure(String id, String actorUserId, boolean isAdmin) {
        Review existing = getReviewById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Review not found.");
        }

        if (!isAdmin) {
            String normalizedActor = ValidationUtils.normalizeRequired(actorUserId, "User");
            if (!normalizedActor.equals(existing.getUserId())) {
                throw new IllegalArgumentException("You can only delete your own review.");
            }
        }

        deleteReview(id);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right.trim());
    }

    private void validateReview(Review review) {
        review.setAgentId(ValidationUtils.normalizeRequired(review.getAgentId(), "Agent ID"));
        review.setUserId(ValidationUtils.normalizeRequired(review.getUserId(), "User ID"));
        review.setComment(ValidationUtils.normalizeRequired(review.getComment(), "Review comment"));

        if (!agentExists(review.getAgentId())) {
            throw new IllegalArgumentException("Selected agent does not exist.");
        }
        if (!userExists(review.getUserId())) {
            throw new IllegalArgumentException("Selected user does not exist.");
        }
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
