package com.memorywedding.domain.enums;

public enum OAuthProvider {
    GOOGLE,
    NAVER;

    public static OAuthProvider fromRegistrationId(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> GOOGLE;
            case "naver" -> NAVER;
            default -> throw new IllegalArgumentException("Unknown provider: " + registrationId);
        };
    }
}
