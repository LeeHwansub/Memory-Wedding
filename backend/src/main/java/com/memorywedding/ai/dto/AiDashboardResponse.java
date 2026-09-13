package com.memorywedding.ai.dto;

import java.util.List;

public record AiDashboardResponse(
        AiJobResponse latestJob,
        List<AiPhotoResultResponse> results,
        List<AiPhotoResultResponse> bestShots,
        String analyzerMode
) {
}
