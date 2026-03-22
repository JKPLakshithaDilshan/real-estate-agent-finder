package com.realestate.app.service;

import com.realestate.app.model.Admin;
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
public class AdminService {
    private static final String FILE_PATH = "src/main/resources/data/admins.txt";
    private static final Pattern ADMIN_ID_PATTERN = Pattern.compile("AD(\\d+)");

    public List<Admin> getAllAdmins() {
        return loadAdmins(true);
    }

    public Admin getAdminById(String id) {
        return getAllAdmins().stream()
                .filter(a -> a.getId().equals(id))
                .findFirst().orElse(null);
    }

    public Admin getAdminByEmail(String email) {
        return getAllAdmins().stream()
                .filter(a -> a.getEmail() != null && a.getEmail().equalsIgnoreCase(email))
                .findFirst().orElse(null);
    }

    public List<Admin> searchAdmins(String query) {
        if (query == null || query.isBlank()) {
            return getAllAdmins();
        }

        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return getAllAdmins().stream()
                .filter(a -> containsIgnoreCase(a.getId(), normalized)
                        || containsIgnoreCase(a.getName(), normalized)
                        || containsIgnoreCase(a.getEmail(), normalized))
                .collect(Collectors.toList());
    }

    public void saveAdmin(Admin admin) {
        normalizeAndValidate(admin);
        if (getAdminByEmail(admin.getEmail()) != null) {
            throw new IllegalArgumentException("An admin with this email already exists.");
        }

        List<Admin> admins = loadAdmins(true);
        admin.setId(generateNextAdminId(admins));
        if (admin.getAdminLevel() == null || admin.getAdminLevel().isBlank()) {
            admin.setAdminLevel("STANDARD");
        }
        admins.add(admin);
        FileHandler.writeAdmins(FILE_PATH, admins);
    }

    public void updateAdmin(Admin updated) {
        normalizeAndValidate(updated);

        if (ValidationUtils.normalize(updated.getId()) == null || updated.getId().isBlank()) {
            throw new IllegalArgumentException("Admin ID is required for update.");
        }

        Admin duplicateByEmail = getAdminByEmail(updated.getEmail());
        if (duplicateByEmail != null && !duplicateByEmail.getId().equals(updated.getId())) {
            throw new IllegalArgumentException("An admin with this email already exists.");
        }

        List<Admin> admins = loadAdmins(true);
        int updatedCount = 0;
        for (int i = 0; i < admins.size(); i++) {
            Admin current = admins.get(i);
            if (updated.getId().equals(current.getId())) {
                admins.set(i, updated);
                updatedCount++;
            }
        }

        if (updatedCount == 0) {
            throw new IllegalArgumentException("Admin not found.");
        }
        if (updatedCount > 1) {
            throw new IllegalStateException("Duplicate admin IDs detected. IDs must be unique before update.");
        }

        FileHandler.writeAdmins(FILE_PATH, admins);
    }

    public void deleteAdmin(String id) {
        String normalizedId = ValidationUtils.normalizeRequired(id, "Admin ID");
        List<Admin> admins = loadAdmins(true);

        int before = admins.size();
        admins.removeIf(a -> normalizedId.equals(a.getId()));
        int removed = before - admins.size();

        if (removed == 0) {
            throw new IllegalArgumentException("Admin not found.");
        }
        if (removed > 1) {
            throw new IllegalStateException("Duplicate admin IDs detected. IDs must be unique before delete.");
        }

        FileHandler.writeAdmins(FILE_PATH, admins);
    }

    private List<Admin> loadAdmins(boolean rewriteIfChanged) {
        List<Admin> stored = FileHandler.readAdmins(FILE_PATH);
        if (stored.isEmpty()) {
            return stored;
        }

        Map<String, Admin> uniqueById = new LinkedHashMap<>();
        List<Admin> normalized = new ArrayList<>();
        boolean changed = false;

        for (Admin admin : stored) {
            String id = ValidationUtils.normalize(admin.getId());
            if (id == null || id.isBlank() || uniqueById.containsKey(id)) {
                String nextId = generateNextAdminId(normalized);
                if (!Objects.equals(id, nextId)) {
                    changed = true;
                }
                admin.setId(nextId);
                id = nextId;
            }

            Admin existing = uniqueById.get(id);
            if (existing != null) {
                if (sameAdminRecord(existing, admin)) {
                    changed = true;
                    continue;
                }

                String reassignedId = generateNextAdminId(normalized);
                admin.setId(reassignedId);
                changed = true;
            }

            uniqueById.put(admin.getId(), admin);
            normalized.add(admin);
        }

        if (rewriteIfChanged && changed) {
            FileHandler.writeAdmins(FILE_PATH, normalized);
        }
        return normalized;
    }

    private boolean sameAdminRecord(Admin left, Admin right) {
        return Objects.equals(left.toFileString(), right.toFileString());
    }

    private String generateNextAdminId(List<Admin> admins) {
        int maxId = admins.stream()
                .map(Admin::getId)
                .map(this::extractNumericAdminId)
                .max(Integer::compareTo)
                .orElse(0);
        return String.format("AD%03d", maxId + 1);
    }

    private int extractNumericAdminId(String id) {
        if (id == null) {
            return 0;
        }
        Matcher matcher = ADMIN_ID_PATTERN.matcher(id.trim().toUpperCase(Locale.ROOT));
        if (!matcher.matches()) {
            return 0;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private boolean containsIgnoreCase(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private void normalizeAndValidate(Admin admin) {
        admin.setName(ValidationUtils.normalizeRequired(admin.getName(), "Name"));
        admin.setEmail(ValidationUtils.normalizeRequired(admin.getEmail(), "Email"));
        admin.setPhone(ValidationUtils.normalizeRequired(admin.getPhone(), "Phone number"));
        admin.setPassword(ValidationUtils.normalizeRequired(admin.getPassword(), "Password"));
        admin.setAdminLevel(ValidationUtils.normalize(admin.getAdminLevel()));

        ValidationUtils.validateLength(admin.getName(), "Name", 3, 60);
        ValidationUtils.validateEmail(admin.getEmail());
        ValidationUtils.validatePhone(admin.getPhone());
        ValidationUtils.validatePasswordStrength(admin.getPassword());
    }
}
