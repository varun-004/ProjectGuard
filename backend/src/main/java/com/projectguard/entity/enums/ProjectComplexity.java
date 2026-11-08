package com.projectguard.entity.enums;

public enum ProjectComplexity {
    EASY,
    MEDIUM,
    HARD;

    public static ProjectComplexity fromString(String val) {
        if (val == null || val.isBlank()) {
            return MEDIUM;
        }
        String upper = val.trim().toUpperCase();
        return switch (upper) {
            case "EASY", "BEGINNER", "LOW" -> EASY;
            case "HARD", "ADVANCED", "HIGH" -> HARD;
            default -> MEDIUM;
        };
    }
}

