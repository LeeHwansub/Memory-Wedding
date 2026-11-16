package com.memorywedding.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorywedding.config.GeminiProperties;
import com.memorywedding.domain.enums.SceneCategory;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 하이라이트용 LLM 자막·BGM 분위기 추천 (FR-AI-012·013 고도화 1차).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HighlightNarrationService {

    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    public boolean isLiveGemini() {
        return geminiProperties.getApiKey() != null && !geminiProperties.getApiKey().isBlank();
    }

    public boolean isBundledBgmAvailable() {
        try {
            return new ClassPathResource("bgm/default.mp3").exists();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 장면 메타로 짧은 한글 자막 생성. 실패 시 템플릿 폴백.
     */
    public String generateCaption(
            SceneCategory category,
            List<String> people,
            List<String> objects,
            String place) {
        if (isLiveGemini()) {
            try {
                String caption = callCaptionGemini(category, people, objects, place);
                if (caption != null && !caption.isBlank()) {
                    return truncate(caption.trim(), 28);
                }
            } catch (Exception e) {
                log.warn("Caption Gemini failed, mock fallback: {}", e.getMessage());
            }
        }
        return mockCaption(category, people, place);
    }

    /**
     * Best Shot 장면 구성으로 분위기 → 스타일 추천.
     */
    public HighlightOptions.Style recommendStyle(List<SceneCategory> scenes) {
        if (scenes == null || scenes.isEmpty()) {
            return HighlightOptions.Style.CLASSIC;
        }
        if (isLiveGemini()) {
            try {
                HighlightOptions.Style style = callMoodGemini(scenes);
                if (style != null) {
                    return style;
                }
            } catch (Exception e) {
                log.warn("Mood Gemini failed, mock fallback: {}", e.getMessage());
            }
        }
        return mockMood(scenes);
    }

    private String callCaptionGemini(
            SceneCategory category,
            List<String> people,
            List<String> objects,
            String place) throws Exception {
        String prompt = """
                결혼식 하이라이트 영상의 짧은 한글 자막을 만드세요.
                규칙: 한 줄, 최대 20자, 감성적이되 과장 없이, 따옴표/이모지 금지.
                장면=%s, 인물=%s, 객체=%s, 장소=%s
                JSON만 답하세요: {"caption":"..."}
                """.formatted(
                category == null ? "OTHER" : category.name(),
                people == null ? List.of() : people,
                objects == null ? List.of() : objects,
                place == null ? "" : place);

        String text = callGeminiText(prompt);
        JsonNode node = objectMapper.readTree(stripFences(text));
        return node.path("caption").asText(null);
    }

    private HighlightOptions.Style callMoodGemini(List<SceneCategory> scenes) throws Exception {
        Map<SceneCategory, Integer> counts = new EnumMap<>(SceneCategory.class);
        for (SceneCategory scene : scenes) {
            if (scene != null) {
                counts.merge(scene, 1, Integer::sum);
            }
        }
        String prompt = """
                결혼식 하이라이트 BGM/연출 분위기를 고르세요.
                장면 구성(건수): %s
                soft=감성·잔잔, classic=기본·따뜻, cinematic=영화적·드라마틱
                JSON만: {"mood":"soft|classic|cinematic"}
                """.formatted(counts);

        String text = callGeminiText(prompt);
        JsonNode node = objectMapper.readTree(stripFences(text));
        String mood = node.path("mood").asText("classic").toLowerCase(Locale.ROOT);
        return switch (mood) {
            case "soft", "감성" -> HighlightOptions.Style.SOFT;
            case "cinematic", "시네마틱" -> HighlightOptions.Style.CINEMATIC;
            default -> HighlightOptions.Style.CLASSIC;
        };
    }

    private String callGeminiText(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiProperties.getModel()
                + ":generateContent?key="
                + geminiProperties.getApiKey();

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))));

        RestClient client = restClientBuilder.build();
        String response = client.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(response);
            return root.path("candidates").path(0).path("content").path("parts").path(0)
                    .path("text").asText("");
        } catch (Exception e) {
            throw new IllegalStateException("Gemini response parse failed", e);
        }
    }

    private String mockCaption(SceneCategory category, List<String> people, String place) {
        String who = (people != null && !people.isEmpty()) ? people.get(0) : null;
        String where = (place != null && !place.isBlank()) ? place : null;
        return switch (category == null ? SceneCategory.OTHER : category) {
            case ENTRANCE -> who != null ? who + "의 첫 걸음" : "설레는 입장";
            case SONG -> "마음을 담은 축가";
            case GROUP_PHOTO -> "함께한 이 순간";
            case RECEPTION -> where != null ? where + "의 축복" : "따뜻한 피로연";
            case OTHER -> "오늘의 기억";
        };
    }

    private HighlightOptions.Style mockMood(List<SceneCategory> scenes) {
        long reception = scenes.stream().filter(s -> s == SceneCategory.RECEPTION).count();
        long song = scenes.stream().filter(s -> s == SceneCategory.SONG).count();
        long entrance = scenes.stream().filter(s -> s == SceneCategory.ENTRANCE).count();
        if (reception >= song && reception >= entrance && reception > 0) {
            return HighlightOptions.Style.CINEMATIC;
        }
        if (song > 0 || entrance > 0) {
            return HighlightOptions.Style.SOFT;
        }
        return HighlightOptions.Style.CLASSIC;
    }

    private String stripFences(String text) {
        if (text == null) {
            return "";
        }
        String t = text.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```(?:json)?\\s*", "");
            t = t.replaceFirst("\\s*```$", "");
        }
        return t.trim();
    }

    private String truncate(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }
}
