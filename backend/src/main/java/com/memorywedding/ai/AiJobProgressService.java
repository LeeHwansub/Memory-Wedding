package com.memorywedding.ai;

import com.memorywedding.domain.entity.AiAnalysisJob;
import com.memorywedding.domain.entity.AiVideoJob;
import com.memorywedding.domain.repository.AiAnalysisJobRepository;
import com.memorywedding.domain.repository.AiVideoJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiJobProgressService {

    private final AiAnalysisJobRepository aiAnalysisJobRepository;
    private final AiVideoJobRepository aiVideoJobRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void bumpAnalysisProcessed(Long jobId) {
        AiAnalysisJob job = aiAnalysisJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        job.incrementProcessed();
        aiAnalysisJobRepository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void bumpHighlightProcessed(Long jobId) {
        AiVideoJob job = aiVideoJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        job.incrementProcessedClips();
        aiVideoJobRepository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAnalysisFailed(Long jobId, String message) {
        AiAnalysisJob job = aiAnalysisJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        job.markFailed(message);
        aiAnalysisJobRepository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markHighlightFailed(Long jobId, String message) {
        AiVideoJob job = aiVideoJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        job.markFailed(message);
        aiVideoJobRepository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeHighlight(
            Long jobId, String storageProvider, String storageKey, long fileSize) {
        AiVideoJob job = aiVideoJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        job.markCompleted(storageProvider, storageKey, fileSize);
        aiVideoJobRepository.save(job);
    }
}
