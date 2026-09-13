package com.memorywedding.controller;

import com.memorywedding.ai.AiAnalysisService;
import com.memorywedding.ai.AiHighlightService;
import com.memorywedding.ai.dto.AiDashboardResponse;
import com.memorywedding.ai.dto.AiVideoJobResponse;
import com.memorywedding.common.ApiResponse;
import com.memorywedding.domain.entity.AiVideoJob;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AiController {

    private final AiAnalysisService aiAnalysisService;
    private final AiHighlightService aiHighlightService;

    @GetMapping("/api/projects/{projectId}/ai")
    public ApiResponse<AiDashboardResponse> dashboard(
            Authentication authentication,
            @PathVariable Long projectId) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(aiAnalysisService.getDashboard(memberId, projectId));
    }

    @PostMapping("/api/projects/{projectId}/ai/analyze")
    public ApiResponse<AiDashboardResponse> analyze(
            Authentication authentication,
            @PathVariable Long projectId) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(
                aiAnalysisService.startAnalysis(memberId, projectId),
                "AI 분석이 완료되었습니다.");
    }

    @GetMapping("/api/projects/{projectId}/ai/video")
    public ApiResponse<AiVideoJobResponse> latestVideo(
            Authentication authentication,
            @PathVariable Long projectId) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(aiHighlightService.getLatest(memberId, projectId));
    }

    @PostMapping("/api/projects/{projectId}/ai/video")
    public ApiResponse<AiVideoJobResponse> createVideo(
            Authentication authentication,
            @PathVariable Long projectId) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(
                aiHighlightService.createHighlight(memberId, projectId),
                "하이라이트 영상 생성이 완료되었습니다.");
    }

    @GetMapping("/api/projects/{projectId}/ai/video/{jobId}/content")
    public ResponseEntity<InputStreamResource> openVideo(
            Authentication authentication,
            @PathVariable Long projectId,
            @PathVariable Long jobId) {
        Long memberId = (Long) authentication.getPrincipal();
        AiVideoJob job = aiHighlightService.getOwnedJob(memberId, projectId, jobId);
        InputStream stream = aiHighlightService.openContent(job);
        long size = job.getFileSize() == null ? -1 : job.getFileSize();
        InputStreamResource body = new InputStreamResource(stream);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"highlight-" + jobId + ".mp4\"")
                .contentType(MediaType.parseMediaType("video/mp4"));
        if (size >= 0) {
            builder.contentLength(size);
        }
        return builder.body(body);
    }
}
