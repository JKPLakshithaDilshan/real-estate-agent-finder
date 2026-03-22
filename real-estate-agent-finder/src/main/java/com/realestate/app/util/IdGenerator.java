package com.realestate.app.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class IdGenerator {
    private static final int ID_DIGITS = 3;
    private static final Map<String, AtomicInteger> COUNTERS = new ConcurrentHashMap<>();

    private static final Map<String, String> PREFIX_MAP = Map.of(
            "ADM", "AD",
            "AGT", "A",
            "USR", "U",
            "APT", "AP",
            "RTG", "R",
            "RVW", "RV"
    );

    public static String generate(String prefix) {
        String normalizedPrefix = normalizePrefix(prefix);
        AtomicInteger counter = COUNTERS.computeIfAbsent(normalizedPrefix, key -> new AtomicInteger(0));
        int nextValue = counter.incrementAndGet();
        return normalizedPrefix + String.format("%0" + ID_DIGITS + "d", nextValue);
    }

    public static String generateAdminId() {
        return generate("AD");
    }

    public static String generateAgentId() {
        return generate("A");
    }

    public static String generateUserId() {
        return generate("U");
    }

    public static String generateAppointmentId() {
        return generate("AP");
    }

    public static String generateRatingId() {
        return generate("R");
    }

    public static String generateReviewId() {
        return generate("RV");
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("Prefix cannot be null or blank");
        }

        String cleanedPrefix = prefix.trim().toUpperCase();
        return PREFIX_MAP.getOrDefault(cleanedPrefix, cleanedPrefix);
    }
}
