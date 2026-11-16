package com.memorywedding.drive.dto;

public record ProjectDriveStatusResponse(
        boolean driveConnected,
        String googleAccountEmail,
        boolean projectFolderReady,
        String projectFolderPath,
        long pendingSyncCount,
        long syncedCount
) {
}
