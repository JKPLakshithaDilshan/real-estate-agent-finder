package com.realestate.app.service;

import com.realestate.app.model.User;
import com.realestate.app.util.FileHandler;
import com.realestate.app.util.ValidationUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UserService {
    private static final String FILE_PATH = "src/main/resources/data/users.txt";
    private static final Pattern USER_ID_PATTERN = Pattern.compile("U(\\d+)");

    public List<User> getAllUsers() {
        return loadUsers(true);
    }

    public User getUserById(String id) {
        String normalizedId = ValidationUtils.normalize(id);
        if (normalizedId == null || normalizedId.isBlank()) {
            return null;
        }

        return getAllUsers().stream()
                .filter(user -> normalizedId.equals(user.getId()))
                .findFirst()
                .orElse(null);
    }

    public User getUserByEmail(String email) {
        String normalizedEmail = ValidationUtils.normalize(email);
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return null;
        }

        return getAllUsers().stream()
                .filter(user -> user.getEmail() != null && user.getEmail().equalsIgnoreCase(normalizedEmail))
                .findFirst()
                .orElse(null);
    }

    public User getUserByUsername(String username) {
        String normalizedUsername = ValidationUtils.normalize(username);
        if (normalizedUsername == null || normalizedUsername.isBlank()) {
            return null;
        }

        return getAllUsers().stream()
                .filter(user -> user.getUsername() != null && user.getUsername().equalsIgnoreCase(normalizedUsername))
                .findFirst()
                .orElse(null);
    }

    public User findByLoginIdentifier(String identifier) {
        String normalizedIdentifier = ValidationUtils.normalize(identifier);
        if (normalizedIdentifier == null || normalizedIdentifier.isBlank()) {
            return null;
        }

        User byEmail = getUserByEmail(normalizedIdentifier);
        if (byEmail != null) {
            return byEmail;
        }
        return getUserByUsername(normalizedIdentifier);
    }

    public List<User> searchUsers(String query) {
        if (query == null || query.isBlank()) {
            return getAllUsers();
        }

        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return getAllUsers().stream()
                .filter(user -> containsIgnoreCase(user.getId(), normalized)
                        || containsIgnoreCase(user.getUsername(), normalized)
                        || containsIgnoreCase(user.getEmail(), normalized))
                .collect(Collectors.toList());
    }

    public void saveUser(User user) {
        List<User> users = loadUsers(true);
        normalizeAndValidate(user);

        if (hasDuplicateEmail(users, user.getEmail(), null)) {
            throw new IllegalArgumentException("A user with this email already exists.");
        }
        if (hasDuplicateUsername(users, user.getUsername(), null)) {
            throw new IllegalArgumentException("A user with this username already exists.");
        }

        user.setId(generateNextUserId(users));
        user.setActive(true);
        users.add(user);
        FileHandler.writeUsers(FILE_PATH, users);
    }

    public void updateUser(User updated) {
        List<User> users = loadUsers(true);
        normalizeAndValidate(updated);

        if (updated.getId() == null || updated.getId().isBlank()) {
            throw new IllegalArgumentException("User id is required.");
        }
        if (hasDuplicateEmail(users, updated.getEmail(), updated.getId())) {
            throw new IllegalArgumentException("A user with this email already exists.");
        }
        if (hasDuplicateUsername(users, updated.getUsername(), updated.getId())) {
            throw new IllegalArgumentException("A user with this username already exists.");
        }

        int targetIndex = -1;
        for (int index = 0; index < users.size(); index++) {
            if (Objects.equals(updated.getId(), users.get(index).getId())) {
                if (targetIndex >= 0) {
                    throw new IllegalStateException("Duplicate user ids detected in file storage. Resolve duplicates before updating users.");
                }
                targetIndex = index;
            }
        }

        if (targetIndex < 0) {
            throw new IllegalArgumentException("User not found.");
        }

        users.set(targetIndex, updated);
        FileHandler.writeUsers(FILE_PATH, users);
    }

    public void deleteUser(String id) {
        List<User> users = loadUsers(true);
        int matchedUsers = 0;
        List<User> remainingUsers = new ArrayList<>();

        for (User user : users) {
            if (Objects.equals(user.getId(), id)) {
                matchedUsers++;
                continue;
            }
            remainingUsers.add(user);
        }

        if (matchedUsers == 0) {
            throw new IllegalArgumentException("User not found.");
        }
        if (matchedUsers > 1) {
            throw new IllegalStateException("Duplicate user ids detected in file storage. Resolve duplicates before deleting users.");
        }

        FileHandler.writeUsers(FILE_PATH, remainingUsers);
    }

    public void deactivateUser(String id) {
        User existing = getUserById(id);
        if (existing == null) {
            throw new IllegalArgumentException("User not found.");
        }

        existing.setActive(false);
        updateUser(existing);
    }

    private List<User> loadUsers(boolean rewriteIfChanged) {
        List<User> storedUsers = FileHandler.readUsers(FILE_PATH);
        Map<String, User> uniqueUsers = new LinkedHashMap<>();
        boolean changed = false;

        for (User user : storedUsers) {
            User existing = uniqueUsers.get(user.getId());
            if (existing == null) {
                uniqueUsers.put(user.getId(), user);
                continue;
            }

            if (sameUserRecord(existing, user)) {
                changed = true;
                continue;
            }

            throw new IllegalStateException("Duplicate user id detected for different accounts: " + user.getId());
        }

        List<User> normalizedUsers = new ArrayList<>(uniqueUsers.values());
        if (rewriteIfChanged && changed) {
            FileHandler.writeUsers(FILE_PATH, normalizedUsers);
        }
        return normalizedUsers;
    }

    private boolean sameUserRecord(User left, User right) {
        return Objects.equals(left.toFileString(), right.toFileString());
    }

    private String generateNextUserId(List<User> users) {
        int maxId = users.stream()
                .map(User::getId)
                .map(this::extractNumericUserId)
                .max(Integer::compareTo)
                .orElse(0);
        return String.format("U%03d", maxId + 1);
    }

    private int extractNumericUserId(String id) {
        if (id == null) {
            return 0;
        }

        Matcher matcher = USER_ID_PATTERN.matcher(id.trim().toUpperCase(Locale.ROOT));
        if (!matcher.matches()) {
            return 0;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private boolean hasDuplicateEmail(List<User> users, String email, String existingUserId) {
        return users.stream().anyMatch(user -> user.getEmail() != null
                && user.getEmail().equalsIgnoreCase(email)
                && !Objects.equals(user.getId(), existingUserId));
    }

    private boolean hasDuplicateUsername(List<User> users, String username, String existingUserId) {
        return users.stream().anyMatch(user -> user.getUsername() != null
                && user.getUsername().equalsIgnoreCase(username)
                && !Objects.equals(user.getId(), existingUserId));
    }

    private boolean containsIgnoreCase(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private void normalizeAndValidate(User user) {
        String normalizedUsername = ValidationUtils.normalizeRequired(user.getUsername(), "Username");
        user.setUsername(normalizedUsername);
        user.setEmail(ValidationUtils.normalizeRequired(user.getEmail(), "Email"));
        user.setPhone(ValidationUtils.normalizeRequired(user.getPhone(), "Phone number"));
        user.setPassword(ValidationUtils.normalizeRequired(user.getPassword(), "Password"));
        user.setPreferredLocation(ValidationUtils.normalize(user.getPreferredLocation()));
        user.setPropertyType(ValidationUtils.normalize(user.getPropertyType()));

        ValidationUtils.validateLength(user.getUsername(), "Username", 3, 50);
        ValidationUtils.validateEmail(user.getEmail());
        ValidationUtils.validatePhone(user.getPhone());
        ValidationUtils.validatePasswordStrength(user.getPassword());
    }
}
