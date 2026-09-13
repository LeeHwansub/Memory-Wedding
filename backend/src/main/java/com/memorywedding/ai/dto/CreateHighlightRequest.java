package com.memorywedding.ai.dto;

/**
 * 하이라이트 생성 옵션 (FR-AI-012~015 1차).
 * null/빈 값은 서버 기본값으로 채웁니다.
 */
public record CreateHighlightRequest(
        String style,
        String length,
        Boolean bgm,
        Boolean subtitles
) {
}
