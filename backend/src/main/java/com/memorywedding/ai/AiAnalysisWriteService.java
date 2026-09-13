package com.memorywedding.ai;

import com.memorywedding.config.GeminiProperties;
import com.memorywedding.domain.entity.AiAnalysisJob;
import com.memorywedding.domain.entity.AiPhotoResult;
import com.memorywedding.domain.entity.UploadFile;
import com.memorywedding.domain.enums.FileType;
import com.memorywedding.domain.enums.SceneCategory;
import com.memorywedding.domain.enums.UploadStatus;
import com.memorywedding.domain.repository.AiAnalysisJobRepository;
import com.memorywedding.domain.repository.AiPhotoResultRepository;
import com.memorywedding.domain.repository.UploadFileRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiAnalysisWriteService {

    private final AiAnalysisJobRepository aiAnalysisJobRepository;
    private final AiPhotoResultRepository aiPhotoResultRepository;
    private final UploadFileRepository uploadFileRepository;
    private final GeminiProperties geminiProperties;

    @Transactional(readOnly = true)
    public Long requireProjectId(Long jobId) {
        AiAnalysisJob job = aiAnalysisJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("AI Job not found: " + jobId));
        return job.getProject().getId();
    }

    @Transactional(readOnly = true)
    public List<UploadFile> resolveTargets(Long projectId) {
        List<UploadFile> photos = uploadFileRepository
                .findByProject_IdAndFileTypeAndUploadStatusAndDeletedAtIsNullOrderByCreatedAtAsc(
                        projectId, FileType.PHOTO, UploadStatus.COMPLETED);
        List<UploadFile> videos = uploadFileRepository
                .findByProject_IdAndFileTypeAndUploadStatusAndDeletedAtIsNullOrderByCreatedAtAsc(
                        projectId, FileType.VIDEO, UploadStatus.COMPLETED);

        int photoLimit = Math.max(1, geminiProperties.getMaxPhotos());
        int videoLimit = Math.max(0, geminiProperties.getMaxVideos());
        List<UploadFile> photoTargets =
                photos.size() > photoLimit ? photos.subList(0, photoLimit) : photos;
        List<UploadFile> videoTargets =
                videos.size() > videoLimit ? videos.subList(0, videoLimit) : videos;

        List<UploadFile> targets = new ArrayList<>(photoTargets.size() + videoTargets.size());
        targets.addAll(photoTargets);
        targets.addAll(videoTargets);
        for (UploadFile file : targets) {
            file.getStorageKey();
            file.getMimeType();
            file.getOriginalFilename();
            file.getFileType();
        }
        return targets;
    }

    @Transactional
    public AiPhotoResult saveResult(
            Long jobId,
            UploadFile file,
            SceneCategory category,
            BigDecimal confidence,
            String metadata) {
        AiAnalysisJob job = aiAnalysisJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("AI Job not found: " + jobId));
        AiPhotoResult result = aiPhotoResultRepository.findByUploadFile_Id(file.getId())
                .map(existing -> {
                    existing.rebind(job, category, false, confidence, metadata);
                    return existing;
                })
                .orElseGet(() -> AiPhotoResult.builder()
                        .job(job)
                        .uploadFile(file)
                        .sceneCategory(category)
                        .bestShot(false)
                        .confidence(confidence)
                        .metadataJson(metadata)
                        .build());
        return aiPhotoResultRepository.save(result);
    }

    @Transactional
    public void finalizeAnalysis(Long jobId) {
        AiAnalysisJob job = aiAnalysisJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("AI Job not found: " + jobId));
        List<AiPhotoResult> results = aiPhotoResultRepository
                .findByJob_IdOrderBySceneCategoryAscIdAsc(jobId);
        markBestShots(results);
        results.forEach(aiPhotoResultRepository::save);

        if (job.getProcessedFiles() == 0) {
            job.markFailed("분석에 성공한 사진·영상이 없습니다.");
        } else {
            job.markCompleted();
        }
        aiAnalysisJobRepository.save(job);
    }

    private void markBestShots(List<AiPhotoResult> results) {
        Map<SceneCategory, List<AiPhotoResult>> byScene = new EnumMap<>(SceneCategory.class);
        for (AiPhotoResult result : results) {
            result.clearBestShot();
            if (!meetsConfidence(result)) {
                continue;
            }
            byScene.computeIfAbsent(result.getSceneCategory(), key -> new ArrayList<>()).add(result);
        }
        for (List<AiPhotoResult> group : byScene.values()) {
            group.stream()
                    .max(Comparator.comparing(
                            (AiPhotoResult r) -> r.getConfidence() == null
                                    ? BigDecimal.ZERO
                                    : r.getConfidence()))
                    .ifPresent(AiPhotoResult::markBestShot);
        }
    }

    private boolean meetsConfidence(AiPhotoResult result) {
        BigDecimal min = BigDecimal.valueOf(geminiProperties.getMinConfidence());
        return result.getConfidence() != null && result.getConfidence().compareTo(min) >= 0;
    }
}
