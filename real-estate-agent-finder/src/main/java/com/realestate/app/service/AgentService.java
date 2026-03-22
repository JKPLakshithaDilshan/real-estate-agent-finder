package com.realestate.app.service;

import com.realestate.app.model.Agent;
import com.realestate.app.util.AgentBST;
import com.realestate.app.util.FileHandler;
import com.realestate.app.util.ValidationUtils;
import com.realestate.app.util.SelectionSortUtil;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AgentService {
    private static final String FILE_PATH = "src/main/resources/data/agents.txt";
    private static final String IMAGES_DIR = "src/main/resources/static/images/agents/";
    private static final long MAX_IMAGE_BYTES = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/gif");
    private static final Pattern AGENT_ID_PATTERN = Pattern.compile("A(\\d+)");
    private final RatingService ratingService;

    public AgentService(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    public List<Agent> getAllAgents() {
        List<Agent> agents = loadAgents(true);
        populateRatings(agents);
        return agents;
    }

    public List<Agent> getApprovedAgents() {
        return getAllAgents().stream()
                .filter(agent -> Agent.STATUS_APPROVED.equals(agent.getApprovalStatus()))
                .collect(Collectors.toList());
    }

    public List<Agent> getPendingAgents() {
        return getAllAgents().stream()
                .filter(agent -> Agent.STATUS_PENDING.equals(agent.getApprovalStatus()))
                .collect(Collectors.toList());
    }

    public Agent getAgentById(String id) {
        AgentBST bst = buildAgentBst(getAllAgents());
        return bst.search(id);
    }

    public Agent getApprovedAgentById(String id) {
        Agent agent = getAgentById(id);
        if (agent == null || !Agent.STATUS_APPROVED.equals(agent.getApprovalStatus())) {
            return null;
        }
        return agent;
    }

    public List<Agent> searchAgents(String query) {
        List<Agent> agents = getAllAgents();
        if (query == null || query.isBlank()) {
            return agents;
        }

        String trimmedQuery = query.trim();

        // Use BST specifically for fast lookup by agent ID.
        Agent idMatch = buildAgentBst(agents).search(trimmedQuery);
        if (idMatch != null) {
            return List.of(idMatch);
        }

        // Use simple filtering for non-ID fields like name and location.
        String normalized = trimmedQuery.toLowerCase(Locale.ROOT);
        return agents.stream()
                .filter(agent -> containsIgnoreCase(agent.getName(), normalized)
                        || containsIgnoreCase(agent.getLocation(), normalized)
                        || containsIgnoreCase(agent.getEmail(), normalized)
                        || containsIgnoreCase(agent.getUsername(), normalized)
                        || containsIgnoreCase(agent.getApprovalStatus(), normalized))
                .collect(Collectors.toList());
    }

    public List<Agent> searchApprovedAgents(String query) {
        List<Agent> approvedAgents = getApprovedAgents();
        if (query == null || query.isBlank()) {
            return approvedAgents;
        }

        String trimmedQuery = query.trim();
        Agent idMatch = buildAgentBst(approvedAgents).search(trimmedQuery);
        if (idMatch != null) {
            return List.of(idMatch);
        }

        String normalized = trimmedQuery.toLowerCase(Locale.ROOT);
        return approvedAgents.stream()
                .filter(agent -> containsIgnoreCase(agent.getName(), normalized)
                        || containsIgnoreCase(agent.getLocation(), normalized)
                        || containsIgnoreCase(agent.getSpecialization(), normalized))
                .collect(Collectors.toList());
    }

    public Agent getAgentByEmail(String email) {
        return getAllAgents().stream()
                .filter(a -> a.getEmail() != null && a.getEmail().equalsIgnoreCase(email))
                .findFirst().orElse(null);
    }

    public Agent getAgentByUsername(String username) {
        return getAllAgents().stream()
                .filter(a -> a.getUsername() != null && a.getUsername().equalsIgnoreCase(username))
                .findFirst().orElse(null);
    }

    /**
     * Validates and saves an uploaded profile image to the agents images directory.
     * Returns the generated filename, or null if no file was provided.
     */
    public String saveProfileImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }
        if (imageFile.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Profile image must be 2 MB or smaller.");
        }
        String contentType = imageFile.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Profile image must be a JPEG, PNG, or GIF file.");
        }
        String originalFilename = imageFile.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("Profile image must have a valid file extension.");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Profile image must be a JPEG, PNG, or GIF file.");
        }
        String filename = UUID.randomUUID() + "." + ext;
        Path dir = Paths.get(IMAGES_DIR);
        try {
            Files.createDirectories(dir);
            try (InputStream in = imageFile.getInputStream()) {
                Files.copy(in, dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save profile image. Please try again.", e);
        }
        return filename;
    }

    public void saveAgentApplication(Agent agent) {
        List<Agent> agents = loadAgents(true);
        normalizeAndValidate(agent, false);

        if (hasDuplicateEmail(agents, agent.getEmail(), null)) {
            throw new IllegalArgumentException("An agent with this email already exists.");
        }
        if (hasDuplicateUsername(agents, agent.getUsername(), null)) {
            throw new IllegalArgumentException("An agent with this username already exists.");
        }

        agent.setId(generateNextAgentId(agents));
        agent.setAverageRating(0.0);
        agent.setTotalRatings(0);
        agent.setApprovalStatus(Agent.STATUS_PENDING);
        agent.setCreatedAt(LocalDateTime.now().toString());
        agents.add(agent);
        FileHandler.writeAgents(FILE_PATH, agents);
    }

    public void saveAgentByAdmin(Agent agent) {
        List<Agent> agents = loadAgents(true);
        normalizeAndValidate(agent, false);

        if (hasDuplicateEmail(agents, agent.getEmail(), null)) {
            throw new IllegalArgumentException("An agent with this email already exists.");
        }
        if (hasDuplicateUsername(agents, agent.getUsername(), null)) {
            throw new IllegalArgumentException("An agent with this username already exists.");
        }

        agent.setId(generateNextAgentId(agents));
        agent.setAverageRating(0.0);
        agent.setTotalRatings(0);
        agent.setApprovalStatus(Agent.STATUS_APPROVED);
        if (agent.getCreatedAt() == null || agent.getCreatedAt().isBlank()) {
            agent.setCreatedAt(LocalDateTime.now().toString());
        }
        agents.add(agent);
        FileHandler.writeAgents(FILE_PATH, agents);
    }

    public void updateAgent(Agent updated) {
        List<Agent> agents = loadAgents(true);
        normalizeAndValidate(updated, true);

        if (updated.getId() == null || updated.getId().isBlank()) {
            throw new IllegalArgumentException("Agent id is required.");
        }
        if (hasDuplicateEmail(agents, updated.getEmail(), updated.getId())) {
            throw new IllegalArgumentException("An agent with this email already exists.");
        }
        if (hasDuplicateUsername(agents, updated.getUsername(), updated.getId())) {
            throw new IllegalArgumentException("An agent with this username already exists.");
        }

        int targetIndex = -1;
        for (int index = 0; index < agents.size(); index++) {
            if (Objects.equals(agents.get(index).getId(), updated.getId())) {
                if (targetIndex >= 0) {
                    throw new IllegalStateException("Duplicate agent ids detected in file storage. Resolve duplicates before updating agents.");
                }
                targetIndex = index;
            }
        }

        if (targetIndex < 0) {
            throw new IllegalArgumentException("Agent not found.");
        }

        agents.set(targetIndex, updated);
        FileHandler.writeAgents(FILE_PATH, agents);
    }

    public void deleteAgent(String id) {
        List<Agent> agents = loadAgents(true);
        int matchedAgents = 0;
        List<Agent> remainingAgents = new ArrayList<>();

        for (Agent agent : agents) {
            if (Objects.equals(agent.getId(), id)) {
                matchedAgents++;
                continue;
            }
            remainingAgents.add(agent);
        }

        if (matchedAgents == 0) {
            throw new IllegalArgumentException("Agent not found.");
        }
        if (matchedAgents > 1) {
            throw new IllegalStateException("Duplicate agent ids detected in file storage. Resolve duplicates before deleting agents.");
        }

        FileHandler.writeAgents(FILE_PATH, remainingAgents);
    }

    public void approveAgent(String id) {
        updateApprovalStatus(id, Agent.STATUS_APPROVED);
    }

    public void rejectAgent(String id) {
        updateApprovalStatus(id, Agent.STATUS_REJECTED);
    }

    private void updateApprovalStatus(String id, String status) {
        Agent agent = getAgentById(id);
        if (agent == null) {
            throw new IllegalArgumentException("Agent not found.");
        }
        agent.setApprovalStatus(status);
        updateAgent(agent);
    }

    public List<Agent> getTopRatedAgents() {
        List<Agent> agents = getApprovedAgents();
        SelectionSortUtil.sortByRatingDesc(agents);
        return agents;
    }

    public boolean isApprovedAgent(String agentId) {
        Agent agent = getAgentById(agentId);
        return agent != null && Agent.STATUS_APPROVED.equals(agent.getApprovalStatus());
    }

    private void populateRatings(List<Agent> agents) {
        for (Agent agent : agents) {
            List<Integer> scores = ratingService.getRatingsByAgent(agent.getId()).stream()
                    .map(r -> r.getScore())
                    .toList();

            int count = scores.size();
            double average = scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);

            agent.setTotalRatings(count);
            agent.setAverageRating(Math.round(average * 10.0) / 10.0);
        }
    }

    private boolean containsIgnoreCase(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private List<Agent> loadAgents(boolean rewriteIfChanged) {
        List<Agent> storedAgents = FileHandler.readAgents(FILE_PATH);
        Map<String, Agent> uniqueAgents = new LinkedHashMap<>();
        boolean changed = false;

        for (Agent agent : storedAgents) {
            Agent existing = uniqueAgents.get(agent.getId());
            if (existing == null) {
                uniqueAgents.put(agent.getId(), agent);
                continue;
            }

            if (sameAgentRecord(existing, agent)) {
                changed = true;
                continue;
            }

            throw new IllegalStateException("Duplicate agent id detected for different records: " + agent.getId());
        }

        List<Agent> normalizedAgents = new ArrayList<>(uniqueAgents.values());
        if (rewriteIfChanged && changed) {
            FileHandler.writeAgents(FILE_PATH, normalizedAgents);
        }
        return normalizedAgents;
    }

    private boolean sameAgentRecord(Agent left, Agent right) {
        return Objects.equals(left.toFileString(), right.toFileString());
    }

    private String generateNextAgentId(List<Agent> agents) {
        int maxId = agents.stream()
                .map(Agent::getId)
                .map(this::extractNumericAgentId)
                .max(Integer::compareTo)
                .orElse(0);
        return String.format("A%03d", maxId + 1);
    }

    private int extractNumericAgentId(String id) {
        if (id == null) {
            return 0;
        }

        Matcher matcher = AGENT_ID_PATTERN.matcher(id.trim().toUpperCase(Locale.ROOT));
        if (!matcher.matches()) {
            return 0;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private boolean hasDuplicateEmail(List<Agent> agents, String email, String existingAgentId) {
        return agents.stream().anyMatch(agent -> agent.getEmail() != null
                && agent.getEmail().equalsIgnoreCase(email)
                && !Objects.equals(agent.getId(), existingAgentId));
    }

    private boolean hasDuplicateUsername(List<Agent> agents, String username, String existingAgentId) {
        return agents.stream().anyMatch(agent -> agent.getUsername() != null
                && agent.getUsername().equalsIgnoreCase(username)
                && !Objects.equals(agent.getId(), existingAgentId));
    }

    private void normalizeAndValidate(Agent agent, boolean allowBlankPassword) {
        agent.setName(ValidationUtils.normalizeRequired(agent.getName(), "Full name"));
        agent.setUsername(ValidationUtils.normalizeRequired(agent.getUsername(), "Username"));
        agent.setEmail(ValidationUtils.normalizeRequired(agent.getEmail(), "Email"));
        agent.setPhone(ValidationUtils.normalizeRequired(agent.getPhone(), "Phone number"));
        agent.setAddress(ValidationUtils.normalizeRequired(agent.getAddress(), "Address"));
        agent.setNicNumber(ValidationUtils.normalizeRequired(agent.getNicNumber(), "NIC/ID number"));
        agent.setSpecialization(ValidationUtils.normalizeRequired(agent.getSpecialization(), "Specialization"));
        agent.setLocation(ValidationUtils.normalizeRequired(agent.getLocation(), "Location"));
        agent.setBio(ValidationUtils.normalize(agent.getBio()));
        agent.setProfileImage(ValidationUtils.normalize(agent.getProfileImage()));

        String normalizedPassword = ValidationUtils.normalize(agent.getPassword());
        if (allowBlankPassword && (normalizedPassword == null || normalizedPassword.isBlank())) {
            // Keep existing password managed by caller.
        } else {
            agent.setPassword(ValidationUtils.normalizeRequired(normalizedPassword, "Password"));
            ValidationUtils.validatePasswordStrength(agent.getPassword());
        }

        ValidationUtils.validateLength(agent.getName(), "Full name", 3, 60);
        ValidationUtils.validateLength(agent.getUsername(), "Username", 3, 50);
        ValidationUtils.validateEmail(agent.getEmail());
        ValidationUtils.validatePhone(agent.getPhone());
        ValidationUtils.validateLength(agent.getAddress(), "Address", 3, 120);
        ValidationUtils.validateLength(agent.getNicNumber(), "NIC/ID number", 5, 25);
        ValidationUtils.validateLength(agent.getSpecialization(), "Specialization", 2, 80);
        ValidationUtils.validateLength(agent.getLocation(), "Location", 2, 80);
        if (agent.getYearsOfExperience() < 0 || agent.getYearsOfExperience() > 60) {
            throw new IllegalArgumentException("Years of experience must be between 0 and 60.");
        }
    }

    private AgentBST buildAgentBst(List<Agent> agents) {
        AgentBST bst = new AgentBST();
        agents.forEach(bst::insert);
        return bst;
    }
}
