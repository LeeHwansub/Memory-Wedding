package com.memorywedding.ai.dto;

import java.time.LocalDateTime;

/**
 * 소개글(인사말) AI 추천 요청. 비우면 프로젝트 저장값을 사용합니다.
 */
public record GreetingRecommendRequest(
        String groomName,
        String brideName,
        String venueName,
        LocalDateTime weddingAt
) {
}
