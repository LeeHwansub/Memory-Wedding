package com.memorywedding.ai.dto;

import com.memorywedding.domain.enums.AiJobStatus;
import java.time.LocalDateTime;

public record AiJobResponse(
        Long id,
        AiJobStatus status,
        int totalFiles,
        int processedFiles,
        String errorMessage,
        String analyzerMode,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
}
