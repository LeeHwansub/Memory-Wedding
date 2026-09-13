package com.memorywedding.controller;

import com.memorywedding.ai.AiAnalysisService;
import com.memorywedding.ai.dto.AiDashboardResponse;
import com.memorywedding.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AiController {

    private final AiAnalysisService aiAnalysisService;

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
}
