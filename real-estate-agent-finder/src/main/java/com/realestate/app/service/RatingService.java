package com.realestate.app.service;

import com.realestate.app.model.Rating;
import com.realestate.app.model.Agent;
import com.realestate.app.model.User;
import com.realestate.app.util.FileHandler;
import com.realestate.app.util.ValidationUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Service
public class RatingService {
    private static final String FILE_PATH = "src/main/resources/data/ratings.txt";

    public List<Rating> getAllRatings() {
        return FileHandler.readRatings(FILE_PATH);
    }

    public List<Rating> getRatingsByAgent(String agentId) {
        return getAllRatings().stream()
                .filter(r -> r.getAgentId().equals(agentId))
                .collect(Collectors.toList());
    }

    public List<Rating> getRatingsByUser(String userId) {
        return getAllRatings().stream()
                .filter(r -> r.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<Rating> searchRatings(String agentId, String userId) {
        return getAllRatings().stream()
                .filter(r -> isBlank(agentId) || equalsIgnoreCase(r.getAgentId(), agentId))
                .filter(r -> isBlank(userId) || equalsIgnoreCase(r.getUserId(), userId))
                .collect(Collectors.toList());
    }

    public Rating getRatingById(String id) {
        return getAllRatings().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst().orElse(null);
    }

    private String generateNextRatingId(List<Rating> existing) {
        int maxId = existing.stream()
                .map(Rating::getId)
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
        return String.format("R%03d", maxId + 1);
    }

    public void saveRating(Rating rating) {
        validateRating(rating);
        List<Rating> ratings = getAllRatings();

        boolean duplicateForAgent = ratings.stream().anyMatch(existing -> existing.getUserId() != null
                && existing.getAgentId() != null
                && existing.getUserId().equalsIgnoreCase(rating.getUserId())
                && existing.getAgentId().equalsIgnoreCase(rating.getAgentId()));
        if (duplicateForAgent) {
            throw new IllegalArgumentException("You already rated this agent. Please edit your existing rating.");
        }

        rating.setId(generateNextRatingId(ratings));
        ratings.add(rating);
        FileHandler.writeRatings(FILE_PATH, ratings);
    }

    public void updateRating(Rating updated) {
        validateRating(updated);
        List<Rating> ratings = getAllRatings();
        ratings.replaceAll(r -> r.getId().equals(updated.getId()) ? updated : r);
        FileHandler.writeRatings(FILE_PATH, ratings);
    }

    public void updateRatingSecure(Rating updated, String actorUserId, boolean isAdmin) {
        Rating existing = getRatingById(updated.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Rating not found.");
        }

        if (!isAdmin) {
            String normalizedActor = ValidationUtils.normalizeRequired(actorUserId, "User");
            if (!normalizedActor.equals(existing.getUserId())) {
                throw new IllegalArgumentException("You can only update your own rating.");
            }
            updated.setUserId(existing.getUserId());
            updated.setAgentId(existing.getAgentId());
        }

        updateRating(updated);
    }

    public void deleteRating(String id) {
        List<Rating> ratings = getAllRatings();
        ratings.removeIf(r -> r.getId().equals(id));
        FileHandler.writeRatings(FILE_PATH, ratings);
    }

    public void deleteRatingSecure(String id, String actorUserId, boolean isAdmin) {
        Rating existing = getRatingById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Rating not found.");
        }

        if (!isAdmin) {
            String normalizedActor = ValidationUtils.normalizeRequired(actorUserId, "User");
            if (!normalizedActor.equals(existing.getUserId())) {
                throw new IllegalArgumentException("You can only delete your own rating.");
            }
        }

        deleteRating(id);
    }

    public double calculateAverageRating(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return 0.0;
        }
        OptionalDouble avg = getRatingsByAgent(agentId).stream()
                .mapToInt(Rating::getScore)
                .average();
        return Math.round(avg.orElse(0.0) * 10.0) / 10.0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right.trim());
    }

    private void validateRating(Rating rating) {
        rating.setAgentId(ValidationUtils.normalizeRequired(rating.getAgentId(), "Agent ID"));
        rating.setUserId(ValidationUtils.normalizeRequired(rating.getUserId(), "User ID"));
        if (!agentExists(rating.getAgentId())) {
            throw new IllegalArgumentException("Selected agent does not exist.");
        }
        if (!userExists(rating.getUserId())) {
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
