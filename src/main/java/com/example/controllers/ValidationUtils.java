package com.example.controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class ValidationUtils {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );
    
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_]{3,20}$"
    );
    
    private static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static boolean isValidEmail(String email) {
        return email != null && !email.trim().isEmpty() && 
               EMAIL_PATTERN.matcher(email).matches();
    }
    
    public static boolean isValidUsername(String username) {
        return username != null && !username.trim().isEmpty() && 
               USERNAME_PATTERN.matcher(username).matches();
    }
    
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    public static boolean isValidDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return false;
        }
        try {
            LocalDateTime.parse(dateStr, DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
    
    public static LocalDateTime parseDate(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    public static String formatDate(LocalDateTime date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }
    
    public static boolean isValidLength(String value, int minLength, int maxLength) {
        if (value == null) return false;
        int length = value.trim().length();
        return length >= minLength && length <= maxLength;
    }
    
    public static boolean isValidFileSize(long fileSizeBytes, long maxSizeBytes) {
        return fileSizeBytes > 0 && fileSizeBytes <= maxSizeBytes;
    }
    
    public static boolean isValidFileExtension(String fileName, String... allowedExtensions) {
        if (fileName == null || fileName.trim().isEmpty()) return false;
        String extension = getFileExtension(fileName).toLowerCase();
        for (String allowed : allowedExtensions) {
            if (extension.equals(allowed.toLowerCase())) return true;
        }
        return false;
    }
    
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return "";
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }
    
    public static boolean isValidContentLength(String content, int maxLength) {
        return content != null && content.length() <= maxLength;
    }
    
    public static String sanitizeInput(String input) {
        if (input == null) return null;
        return input.trim().replaceAll("[<>\"'&]", "");
    }
    
    public static String getValidationErrorMessage(String fieldName, String error) {
        return "Error in " + fieldName + ": " + error;
    }
}
