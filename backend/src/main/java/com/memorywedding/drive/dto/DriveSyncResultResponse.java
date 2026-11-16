package com.memorywedding.drive.dto;

public record DriveSyncResultResponse(
        int synced,
        int skipped,
        int failed
) {
}
