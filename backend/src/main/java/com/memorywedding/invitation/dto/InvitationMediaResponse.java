package com.memorywedding.invitation.dto;

public record InvitationMediaResponse(
        Long id,
        String mediaType,
        String originalFilename,
        String mimeType,
        long fileSize,
        int sortOrder,
        String contentPath
) {
}
