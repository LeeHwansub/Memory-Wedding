package com.memorywedding.ai;

import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AiAsyncDispatcher {

    private final AiAnalysisService aiAnalysisService;
    private final AiHighlightService aiHighlightService;

    public AiAsyncDispatcher(
            @Lazy AiAnalysisService aiAnalysisService,
            @Lazy AiHighlightService aiHighlightService) {
        this.aiAnalysisService = aiAnalysisService;
        this.aiHighlightService = aiHighlightService;
    }

    @Async("aiTaskExecutor")
    public void runAnalysis(Long jobId) {
        aiAnalysisService.processAnalysisJob(jobId);
    }

    @Async("aiTaskExecutor")
    public void runHighlight(Long jobId) {
        aiHighlightService.processHighlightJob(jobId);
    }
}
