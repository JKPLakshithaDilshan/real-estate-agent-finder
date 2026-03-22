package com.realestate.app.util;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

public final class ValidationUtils {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+0-9()\\-\\s]{7,20}$");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile("[^A-Za-z0-9]");

    private ValidationUtils() {
    }

    public static String normalizeRequired(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return normalized;
    }

    public static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    public static void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(normalizeRequired(email, "Email")).matches()) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
    }

    public static void validatePhone(String phone) {
        String normalizedPhone = normalizeRequired(phone, "Phone number");
        if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            throw new IllegalArgumentException("Enter a valid phone number.");
        }
    }

    public static void validatePasswordStrength(String password) {
        String normalizedPassword = normalizeRequired(password, "Password");
        if (normalizedPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long.");
        }
        if (!UPPERCASE_PATTERN.matcher(normalizedPassword).find()) {
            throw new IllegalArgumentException("Password must include at least one uppercase letter.");
        }
        if (!LOWERCASE_PATTERN.matcher(normalizedPassword).find()) {
            throw new IllegalArgumentException("Password must include at least one lowercase letter.");
        }
        if (!DIGIT_PATTERN.matcher(normalizedPassword).find()) {
            throw new IllegalArgumentException("Password must include at least one number.");
        }
        if (!SPECIAL_PATTERN.matcher(normalizedPassword).find()) {
            throw new IllegalArgumentException("Password must include at least one special character.");
        }
    }

    public static void validatePasswordConfirmation(String password, String confirmPassword) {
        if (!normalizeRequired(password, "Password").equals(normalizeRequired(confirmPassword, "Confirm password"))) {
            throw new IllegalArgumentException("Confirm password must match the password.");
        }
    }

    public static void validateLength(String value, String fieldName, int min, int max) {
        String normalized = normalizeRequired(value, fieldName);
        if (normalized.length() < min || normalized.length() > max) {
            throw new IllegalArgumentException(fieldName + " must be between " + min + " and " + max + " characters.");
        }
    }

    public static void validateAppointmentDateTime(LocalDateTime appointmentDateTime, boolean allowPast) {
        if (appointmentDateTime == null) {
            throw new IllegalArgumentException("Appointment date and time are required.");
        }
        if (!allowPast && appointmentDateTime.isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new IllegalArgumentException("Appointment date and time must be at least 30 minutes in the future.");
        }
    }
}