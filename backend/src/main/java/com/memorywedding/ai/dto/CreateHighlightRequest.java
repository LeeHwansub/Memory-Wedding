package com.memorywedding.ai.dto;

/**
 * 하이라이트 생성 옵션 (FR-AI-012~015).
 * aiRecommend=true 이면 분위기 스타일·BGM·자막을 AI가 보조합니다.
 */
public record CreateHighlightRequest(
        String style,
        String length,
        Boolean bgm,
        Boolean subtitles,
        Boolean aiRecommend
) {
}
