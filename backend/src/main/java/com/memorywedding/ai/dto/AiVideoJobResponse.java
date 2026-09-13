package com.memorywedding.ai.dto;

import com.memorywedding.domain.enums.AiJobStatus;
import java.time.LocalDateTime;

public record AiVideoJobResponse(
        Long id,
        AiJobStatus status,
        int clipCount,
        int processedClips,
        Long fileSize,
        String contentPath,
        String driveFileId,
        boolean driveSynced,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
}
