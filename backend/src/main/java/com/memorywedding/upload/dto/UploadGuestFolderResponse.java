package com.memorywedding.upload.dto;

public record UploadGuestFolderResponse(
        String guestName,
        long fileCount
) {
}
