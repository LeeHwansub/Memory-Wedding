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

    @Transactional(readOnly = true)
    public List<AiPhotoResult> loadBestPhotos(Long projectId) {
        AiAnalysisJob analysis = aiAnalysisJobRepository
                .findFirstByProject_IdOrderByCreatedAtDesc(projectId)
                .orElseThrow(() -> new BadRequestException("먼저 AI 분석을 실행해 주세요."));

        if (analysis.getStatus() != AiJobStatus.COMPLETED) {
            throw new BadRequestException("AI 분석이 완료된 후 하이라이트를 생성할 수 있습니다.");
        }

        return aiPhotoResultRepository
                .findByJob_IdOrderBySceneCategoryAscIdAsc(analysis.getId())
                .stream()
                .filter(AiPhotoResult::isBestShot)
                .filter(this::meetsConfidence)
                .filter(r -> r.getUploadFile().getFileType() == FileType.PHOTO)
                .sorted(Comparator
                        .comparingInt((AiPhotoResult r) -> r.getSceneCategory().ordinal())
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
                })
                .toList();
    }

    private boolean meetsConfidence(AiPhotoResult result) {
        BigDecimal min = BigDecimal.valueOf(geminiProperties.getMinConfidence());
        return result.getConfidence() != null && result.getConfidence().compareTo(min) >= 0;
    }
}
