package com.memorywedding.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorywedding.config.GeminiProperties;
import com.memorywedding.domain.enums.SceneCategory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class PhotoSceneAnalyzer {

    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    public record Analysis(
            SceneCategory category,
            BigDecimal confidence,
            String provider,
            String note,
            List<String> people,
            List<String> objects,
            String place) {
    }

    public boolean isLiveGemini() {
        return geminiProperties.getApiKey() != null && !geminiProperties.getApiKey().isBlank();
    }

    public Analysis analyze(byte[] imageBytes, String mimeType, String filename, int index) {
        if (!isLiveGemini() || imageBytes == null || imageBytes.length == 0) {
            return mockAnalyze(filename, index);
        }
        try {
            return callGemini(imageBytes, mimeType == null ? "image/jpeg" : mimeType);
        } catch (Exception e) {
            log.warn("Gemini analyze failed, fallback to mock: {}", e.getMessage());
            return mockAnalyze(filename, index);
        }
    }

    private Analysis mockAnalyze(String filename, int index) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        SceneCategory category;
        List<String> people = new ArrayList<>();
        List<String> objects = new ArrayList<>();
        String place = "예식장";

        if (lower.contains("entrance") || lower.contains("입장") || lower.contains("groom")
                || lower.contains("bride") || lower.contains("신랑") || lower.contains("신부")) {
            category = SceneCategory.ENTRANCE;
            people.add("신랑·신부");
            objects.add("웨딩홀 입구");
        } else if (lower.contains("song") || lower.contains("축가") || lower.contains("music")) {
            category = SceneCategory.SONG;
            people.add("하객");
            objects.add("마이크");
            place = "예식장";
        } else if (lower.contains("group") || lower.contains("단체") || lower.contains("family")) {
            category = SceneCategory.GROUP_PHOTO;
            people.add("신랑·신부·하객");
            objects.add("단체 배경");
        } else if (lower.contains("reception") || lower.contains("피로연") || lower.contains("party")) {
            category = SceneCategory.RECEPTION;
            people.add("하객");
            objects.add("테이블");
            place = "피로연장";
        } else {
            SceneCategory[] values = SceneCategory.values();
            category = values[Math.floorMod(index, values.length)];
            people.add("하객");
        }

        BigDecimal confidence = BigDecimal.valueOf(0.55 + (index % 5) * 0.08)
                .min(BigDecimal.valueOf(0.92))
                .setScale(4, RoundingMode.HALF_UP);
        return new Analysis(
                category,
                confidence,
                "mock",
                "GEMINI_API_KEY 미설정 또는 호출 실패 시 휴리스틱 분류",
                people,
                objects,
                place);
    }

    private Analysis callGemini(byte[] imageBytes, String mimeType) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiProperties.getModel()
                + ":generateContent?key="
                + geminiProperties.getApiKey();

        String prompt = """
                Analyze this wedding photo.
                Classify into exactly one scene category:
                ENTRANCE (입장), SONG (축가), GROUP_PHOTO (단체사진), RECEPTION (피로연), OTHER (기타).
                Extract people, objects, and place (venue/location hint).
                Reply JSON only:
                {"category":"...","confidence":0.0,"people":["..."],"objects":["..."],"place":"..."}
                """;

        Map<String, Object> body = Map.of(
                "contents", java.util.List.of(
                        Map.of(
                                "parts", java.util.List.of(
                                        Map.of("text", prompt),
                                        Map.of(
                                                "inline_data", Map.of(
                                                        "mime_type", mimeType,
                                                        "data", Base64.getEncoder().encodeToString(imageBytes)
                                                )
                                        )
                                )
                        )
                )
        );

        RestClient client = restClientBuilder.build();
        String response = client.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(response);
        String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        String cleaned = text.replace("```json", "").replace("```", "").trim();
        JsonNode parsed = objectMapper.readTree(cleaned);
        SceneCategory category = parseCategory(parsed.path("category").asText("OTHER"));
        BigDecimal confidence = BigDecimal.valueOf(parsed.path("confidence").asDouble(0.7))
                .setScale(4, RoundingMode.HALF_UP);
        return new Analysis(
                category,
                confidence,
                "gemini",
                geminiProperties.getModel(),
                readStringList(parsed.path("people")),
                readStringList(parsed.path("objects")),
                parsed.path("place").asText(""));
    }

    private SceneCategory parseCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return SceneCategory.OTHER;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return SceneCategory.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return switch (normalized) {
                case "GROOM_ENTRANCE", "BRIDE_ENTRANCE", "입장" -> SceneCategory.ENTRANCE;
                case "CEREMONY", "축가" -> SceneCategory.SONG;
                case "GUESTS", "단체", "단체사진", "GROUP" -> SceneCategory.GROUP_PHOTO;
                case "피로연" -> SceneCategory.RECEPTION;
                default -> SceneCategory.OTHER;
            };
        }
    }

    private List<String> readStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return values;
        }
        node.forEach(item -> {
            if (item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText().trim());
            }
        });
        return values;
    }
}
