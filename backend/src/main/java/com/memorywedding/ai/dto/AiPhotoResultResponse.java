package com.memorywedding.ai.dto;

import com.memorywedding.domain.enums.SceneCategory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AiPhotoResultResponse(
        Long id,
        Long uploadFileId,
        String originalFilename,
        String guestName,
        String contentPath,
        SceneCategory sceneCategory,
        boolean bestShot,
        BigDecimal confidence,
        List<String> people,
        List<String> objects,
        String place,
        LocalDateTime analyzedAt
) {
}
