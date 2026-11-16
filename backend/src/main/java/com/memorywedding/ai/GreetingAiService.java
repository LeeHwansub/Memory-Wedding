package com.memorywedding.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorywedding.ai.dto.GreetingRecommendRequest;
import com.memorywedding.ai.dto.GreetingRecommendResponse;
import com.memorywedding.common.ForbiddenException;
import com.memorywedding.common.NotFoundException;
import com.memorywedding.config.GeminiProperties;
import com.memorywedding.domain.entity.WeddingProject;
import com.memorywedding.domain.repository.WeddingProjectRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/**
 * 청첩장 소개글(인사말) AI 추천.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GreetingAiService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREAN);

    private final WeddingProjectRepository weddingProjectRepository;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    public GreetingRecommendResponse recommend(
            Long memberId, Long projectId, GreetingRecommendRequest request) {
        WeddingProject project = getOwnedProject(memberId, projectId);

        String groom = firstNonBlank(request != null ? request.groomName() : null, project.getGroomName());
        String bride = firstNonBlank(request != null ? request.brideName() : null, project.getBrideName());
        String venue = firstNonBlank(request != null ? request.venueName() : null, project.getVenueName());
        LocalDateTime weddingAt = request != null && request.weddingAt() != null
                ? request.weddingAt()
                : project.getWeddingAt();

        String greeting = generate(groom, bride, venue, weddingAt);
        return new GreetingRecommendResponse(greeting);
    }

    private String generate(String groom, String bride, String venue, LocalDateTime weddingAt) {
        if (isLiveGemini()) {
            try {
                String text = callGemini(groom, bride, venue, weddingAt);
                if (text != null && !text.isBlank()) {
                    return truncate(text.trim(), 280);
                }
            } catch (Exception e) {
                log.warn("Greeting Gemini failed, mock fallback: {}", e.getMessage());
            }
        }
        return mockGreeting(groom, bride, venue, weddingAt);
    }

    private String callGemini(String groom, String bride, String venue, LocalDateTime weddingAt)
            throws Exception {
        String date = weddingAt == null ? "" : weddingAt.format(DATE_FMT);
        String prompt = """
                모바일 청첩장 소개글(인사말)을 한국어로 작성하세요.
                규칙: 2~4문장, 따뜻하고 단정한 톤, 과장·이모지·따옴표 금지, 하객을 초대하는 문장 포함.
                신랑=%s, 신부=%s, 예식일=%s, 예식장=%s
                JSON만: {"greetingMessage":"..."}
                """.formatted(
                nullToEmpty(groom),
                nullToEmpty(bride),
                date,
                nullToEmpty(venue));

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiProperties.getModel()
                + ":generateContent?key="
                + geminiProperties.getApiKey();

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))));

        String response = restClientBuilder.build()
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(response);
        String text = root.path("candidates").path(0).path("content").path("parts").path(0)
                .path("text").asText("");
        JsonNode node = objectMapper.readTree(stripFences(text));
        return node.path("greetingMessage").asText(null);
    }

    private String mockGreeting(String groom, String bride, String venue, LocalDateTime weddingAt) {
        String g = nullToEmpty(groom);
        String b = nullToEmpty(bride);
        String couple = (!g.isEmpty() && !b.isEmpty())
                ? g + " · " + b
                : (!g.isEmpty() ? g : (!b.isEmpty() ? b : "저희"));
        String date = weddingAt == null ? "" : weddingAt.format(DATE_FMT);
        String place = (venue == null || venue.isBlank()) ? "예식장" : venue.trim();

        if (!date.isBlank()) {
            return couple + "의 결혼을 알립니다.\n"
                    + date + " " + place + "에서 소중한 분들과 함께 새로운 시작을 맞이하려 합니다.\n"
                    + "따뜻한 마음으로 축복해 주시면 감사하겠습니다.";
        }
        return couple + "의 결혼을 알립니다.\n"
                + place + "에서 소중한 분들과 함께 새로운 시작을 맞이하려 합니다.\n"
                + "따뜻한 마음으로 축복해 주시면 감사하겠습니다.";
    }

    private boolean isLiveGemini() {
        return geminiProperties.getApiKey() != null && !geminiProperties.getApiKey().isBlank();
    }

    private WeddingProject getOwnedProject(Long memberId, Long projectId) {
        WeddingProject project = weddingProjectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new NotFoundException("Wedding Project not found"));
        if (!project.getOwner().getId().equals(memberId)) {
            throw new ForbiddenException("이 Wedding Project에 접근할 권한이 없습니다.");
        }
        return project;
    }

    private static String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred.trim();
        }
        return fallback == null ? "" : fallback.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String stripFences(String text) {
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

    private static String truncate(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }
}
