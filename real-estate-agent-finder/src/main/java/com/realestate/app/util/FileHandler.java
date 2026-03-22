package com.realestate.app.util;

import com.realestate.app.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles flat-file (CSV) persistence for all domain objects.
 * Delegates serialisation/deserialisation to each model's own
 * fromFileString() / toFileString() methods.
 */
public class FileHandler {

    public static List<String> readAllLines(String path) {
        return readLines(path);
    }

    public static void writeAllLines(String path, List<String> lines) {
        writeLines(path, lines);
    }

    public static void appendLine(String path, String line) {
        appendNewLine(path, line);
    }

    public static void appendNewLine(String path, String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        Path filePath = Paths.get(path);
        ensureParentDirectory(filePath);
        String content = line.trim() + System.lineSeparator();
        try {
            Files.writeString(
                    filePath,
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.err.println("[FileHandler] Error appending to " + path + ": " + e.getMessage());
        }
    }

    public static void replaceFileContent(String path, List<String> newLines) {
        writeLines(path, newLines);
    }

    public static void updateLine(String path, String oldLine, String newLine) {
        if (oldLine == null || newLine == null) {
            return;
        }
        List<String> lines = readLines(path);
        String oldValue = oldLine.trim();
        String newValue = newLine.trim();
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).equals(oldValue)) {
                lines.set(i, newValue);
            }
        }
        writeLines(path, lines);
    }

    public static void deleteMatchingLine(String path, String lineToDelete) {
        if (lineToDelete == null) {
            return;
        }
        List<String> lines = readLines(path);
        lines.removeIf(line -> line.equals(lineToDelete.trim()));
        writeLines(path, lines);
    }

    // ──────────────────────────────────────────────
    // ADMIN
    // ──────────────────────────────────────────────

    public static List<Admin> readAdmins(String path) {
        List<Admin> admins = new ArrayList<>();
        for (String line : readAllLines(path)) {
            admins.add(Admin.fromFileString(line));
        }
        return admins;
    }

    public static void writeAdmins(String path, List<Admin> admins) {
        writeAllLines(path, admins.stream()
                .map(Admin::toFileString)
                .toList());
    }

    // ──────────────────────────────────────────────
    // AGENT
    // ──────────────────────────────────────────────

    public static List<Agent> readAgents(String path) {
        List<Agent> agents = new ArrayList<>();
        for (String line : readAllLines(path)) {
            agents.add(Agent.fromFileString(line));
        }
        return agents;
    }

    public static void writeAgents(String path, List<Agent> agents) {
        writeAllLines(path, agents.stream()
                .map(Agent::toFileString)
                .toList());
    }

    // ──────────────────────────────────────────────
    // USER
    // ──────────────────────────────────────────────

    public static List<User> readUsers(String path) {
        List<User> users = new ArrayList<>();
        for (String line : readAllLines(path)) {
            users.add(User.fromFileString(line));
        }
        return users;
    }

    public static void writeUsers(String path, List<User> users) {
        writeAllLines(path, users.stream()
                .map(User::toFileString)
                .toList());
    }

    // ──────────────────────────────────────────────
    // APPOINTMENT
    // ──────────────────────────────────────────────

    public static List<Appointment> readAppointments(String path) {
        List<Appointment> appointments = new ArrayList<>();
        for (String line : readAllLines(path)) {
            appointments.add(Appointment.fromFileString(line));
        }
        return appointments;
    }

    public static void writeAppointments(String path, List<Appointment> appointments) {
        writeAllLines(path, appointments.stream()
                .map(Appointment::toFileString)
                .toList());
    }

    // ──────────────────────────────────────────────
    // RATING
    // ──────────────────────────────────────────────

    public static List<Rating> readRatings(String path) {
        List<Rating> ratings = new ArrayList<>();
        for (String line : readAllLines(path)) {
            ratings.add(Rating.fromFileString(line));
        }
        return ratings;
    }

    public static void writeRatings(String path, List<Rating> ratings) {
        writeAllLines(path, ratings.stream()
                .map(Rating::toFileString)
                .toList());
    }

    // ──────────────────────────────────────────────
    // REVIEW
    // ──────────────────────────────────────────────

    public static List<Review> readReviews(String path) {
        List<Review> reviews = new ArrayList<>();
        for (String line : readAllLines(path)) {
            reviews.add(Review.fromFileString(line));
        }
        return reviews;
    }

    public static void writeReviews(String path, List<Review> reviews) {
        writeAllLines(path, reviews.stream()
                .map(Review::toFileString)
                .toList());
    }

    // ──────────────────────────────────────────────
    // PRIVATE HELPERS
    // ──────────────────────────────────────────────

    /**
     * Reads all non-blank lines from a file. Returns empty list if file missing.
     */
    private static List<String> readLines(String path) {
        List<String> lines = new ArrayList<>();
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath)) {
            return lines;
        }
        try {
            for (String line : Files.readAllLines(filePath, StandardCharsets.UTF_8)) {
                if (!line.isBlank())
                    lines.add(line.trim());
            }
        } catch (IOException e) {
            System.err.println("[FileHandler] Error reading " + path + ": " + e.getMessage());
        }
        return lines;
    }

    /**
     * Writes a list of strings to a file, one per line, overwriting existing
     * content.
     */
    private static void writeLines(String path, List<String> lines) {
        Path filePath = Paths.get(path);
        ensureParentDirectory(filePath);
        List<String> safeLines = lines == null ? List.of() : lines;
        try {
            Files.write(filePath, safeLines, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            System.err.println("[FileHandler] Error writing " + path + ": " + e.getMessage());
        }
    }

    private static void ensureParentDirectory(Path filePath) {
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            System.err.println("[FileHandler] Error creating parent directory for " + filePath + ": " + e.getMessage());
        }
    }
}
