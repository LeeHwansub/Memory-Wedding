package com.memorywedding.ai;

import com.memorywedding.common.BadRequestException;
import com.memorywedding.common.NotFoundException;
import com.memorywedding.config.GeminiProperties;
import com.memorywedding.domain.entity.AiAnalysisJob;
import com.memorywedding.domain.entity.AiPhotoResult;
import com.memorywedding.domain.entity.AiVideoJob;
import com.memorywedding.domain.enums.AiJobStatus;
import com.memorywedding.domain.enums.FileType;
import com.memorywedding.domain.repository.AiAnalysisJobRepository;
import com.memorywedding.domain.repository.AiPhotoResultRepository;
import com.memorywedding.domain.repository.AiVideoJobRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiHighlightWriteService {

    private final AiVideoJobRepository aiVideoJobRepository;
    private final AiAnalysisJobRepository aiAnalysisJobRepository;
    private final AiPhotoResultRepository aiPhotoResultRepository;
    private final GeminiProperties geminiProperties;

    @Transactional(readOnly = true)
    public Long requireProjectId(Long jobId) {
        AiVideoJob job = aiVideoJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Highlight job not found: " + jobId));
        return job.getProject().getId();
    }

    /**
     * Best Shot 사진·영상을 장면 흐름 순으로 가져옵니다.
     * 영상 개수는 분석용 상한, 전체 클립 수는 maxClips 상한을 따릅니다.
     */
    @Transactional(readOnly = true)
    public List<AiPhotoResult> loadHighlightAssets(Long projectId, int maxClips) {
        AiAnalysisJob analysis = aiAnalysisJobRepository
                .findFirstByProject_IdOrderByCreatedAtDesc(projectId)
                .orElseThrow(() -> new BadRequestException("먼저 AI 분석을 실행해 주세요."));

        if (analysis.getStatus() != AiJobStatus.COMPLETED) {
            throw new BadRequestException("AI 분석이 완료된 후 하이라이트를 생성할 수 있습니다.");
        }

        List<AiPhotoResult> bestShots = aiPhotoResultRepository
                .findByJob_IdOrderBySceneCategoryAscIdAsc(analysis.getId())
                .stream()
                .filter(AiPhotoResult::isBestShot)
                .filter(this::meetsConfidence)
                .filter(r -> !isDuplicate(r))
                .filter(r -> {
                    FileType type = r.getUploadFile().getFileType();
                    return type == FileType.PHOTO || type == FileType.VIDEO;
                })
                .sorted(Comparator
                        .comparingInt((AiPhotoResult r) -> r.getSceneCategory().ordinal())
                        .thenComparingInt((AiPhotoResult r) ->
                                r.getUploadFile().getFileType() == FileType.PHOTO ? 0 : 1)
                        .thenComparing(
                                (AiPhotoResult r) -> r.getConfidence() == null
                                        ? BigDecimal.ZERO
                                        : r.getConfidence(),
                                Comparator.reverseOrder()))
                .peek(r -> {
                    var file = r.getUploadFile();
                    file.getStorageKey();
                    file.getOriginalFilename();
                    file.getMimeType();
                    file.getFileType();
                    r.getSceneCategory();
                })
                .toList();

        int maxVideos = Math.max(0, geminiProperties.getMaxVideos());
        int clipLimit = Math.max(1, maxClips);
        List<AiPhotoResult> selected = new ArrayList<>();
        int videoCount = 0;
        for (AiPhotoResult result : bestShots) {
            if (selected.size() >= clipLimit) {
                break;
            }
            if (result.getUploadFile().getFileType() == FileType.VIDEO) {
                if (videoCount >= maxVideos) {
                    continue;
                }
                videoCount += 1;
            }
            selected.add(result);
        }
        return selected;
    }

    @Transactional(readOnly = true)
    public List<AiPhotoResult> loadHighlightAssets(Long projectId) {
        return loadHighlightAssets(projectId, 20);
    }

    @Transactional(readOnly = true)
    public String requireOptionsJson(Long jobId) {
        AiVideoJob job = aiVideoJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Highlight job not found: " + jobId));
        return job.getOptionsJson();
    }

    private boolean meetsConfidence(AiPhotoResult result) {
        BigDecimal min = BigDecimal.valueOf(geminiProperties.getMinConfidence());
        return result.getConfidence() != null && result.getConfidence().compareTo(min) >= 0;
    }

    private boolean isDuplicate(AiPhotoResult result) {
        String metadata = result.getMetadataJson();
        if (metadata == null || metadata.isBlank()) {
            return false;
        }
        return metadata.contains("\"duplicate\":true") || metadata.contains("\"duplicate\": true");
    }
}
