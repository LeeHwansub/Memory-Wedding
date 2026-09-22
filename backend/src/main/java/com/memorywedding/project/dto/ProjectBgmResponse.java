package com.memorywedding.project.dto;

public record ProjectBgmResponse(
        boolean uploaded,
        String originalFilename,
        String mimeType,
        Long fileSize,
        String contentPath
) {
    public static ProjectBgmResponse empty() {
        return new ProjectBgmResponse(false, null, null, null, null);
    }
}
