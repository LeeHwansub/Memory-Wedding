package com.memorywedding.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorywedding.ai.dto.AiDashboardResponse;
import com.memorywedding.ai.dto.AiJobResponse;
import com.memorywedding.ai.dto.AiPhotoResultResponse;
import com.memorywedding.common.BadRequestException;
import com.memorywedding.common.ForbiddenException;
import com.memorywedding.common.NotFoundException;
import com.memorywedding.config.GeminiProperties;
import com.memorywedding.domain.entity.AiAnalysisJob;
import com.memorywedding.domain.entity.AiPhotoResult;
import com.memorywedding.domain.entity.Member;
import com.memorywedding.domain.entity.UploadFile;
import com.memorywedding.domain.entity.WeddingProject;
import com.memorywedding.domain.enums.FileType;
import com.memorywedding.domain.enums.SceneCategory;
import com.memorywedding.domain.enums.UploadStatus;
import com.memorywedding.domain.repository.AiAnalysisJobRepository;
import com.memorywedding.domain.repository.AiPhotoResultRepository;
import com.memorywedding.domain.repository.MemberRepository;
import com.memorywedding.domain.repository.UploadFileRepository;
import com.memorywedding.domain.repository.WeddingProjectRepository;
import com.memorywedding.storage.ObjectStorage;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final WeddingProjectRepository weddingProjectRepository;
    private final MemberRepository memberRepository;
    private final UploadFileRepository uploadFileRepository;
    private final AiAnalysisJobRepository aiAnalysisJobRepository;
    private final AiPhotoResultRepository aiPhotoResultRepository;
    private final ObjectStorage objectStorage;
    private final PhotoSceneAnalyzer photoSceneAnalyzer;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    @Transactional
    public AiDashboardResponse startAnalysis(Long memberId, Long projectId) {
        WeddingProject project = getOwnedProject(memberId, projectId);
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("Member not found"));

        List<UploadFile> photos = uploadFileRepository
                .findByProject_IdAndFileTypeAndUploadStatusAndDeletedAtIsNullOrderByCreatedAtAsc(
                        projectId, FileType.PHOTO, UploadStatus.COMPLETED);
        if (photos.isEmpty()) {
            throw new BadRequestException("분석할 완료된 사진이 없습니다. 하객 업로드 후 다시 시도해 주세요.");
        }

        int limit = Math.max(1, geminiProperties.getMaxPhotos());
        List<UploadFile> targets = photos.size() > limit ? photos.subList(0, limit) : photos;

        AiAnalysisJob job = aiAnalysisJobRepository.save(
                AiAnalysisJob.builder().project(project).requestedBy(member).build());
        job.markProcessing(targets.size());

        List<AiPhotoResult> draft = new ArrayList<>();
        int index = 0;
        for (UploadFile file : targets) {
            try {
                byte[] bytes = readBytes(file);
                PhotoSceneAnalyzer.Analysis analysis = photoSceneAnalyzer.analyze(
                        bytes, file.getMimeType(), file.getOriginalFilename(), index);
                String metadata = buildMetadataJson(analysis);

                AiPhotoResult result = aiPhotoResultRepository.findByUploadFile_Id(file.getId())
                        .map(existing -> {
                            existing.rebind(job, analysis.category(), false, analysis.confidence(), metadata);
                            return existing;
                        })
                        .orElseGet(() -> AiPhotoResult.builder()
                                .job(job)
                                .uploadFile(file)
                                .sceneCategory(analysis.category())
                                .bestShot(false)
                                .confidence(analysis.confidence())
                                .metadataJson(metadata)
                                .build());
                draft.add(aiPhotoResultRepository.save(result));
                job.incrementProcessed();
            } catch (Exception e) {
                log.warn("Skip AI for upload {}: {}", file.getId(), e.getMessage());
            }
            index += 1;
        }

        markBestShots(draft);
        draft.forEach(aiPhotoResultRepository::save);

        if (job.getProcessedFiles() == 0) {
            job.markFailed("분석에 성공한 사진이 없습니다.");
        } else {
            job.markCompleted();
        }

        return toDashboard(projectId, job);
    }

    @Transactional(readOnly = true)
    public AiDashboardResponse getDashboard(Long memberId, Long projectId) {
        getOwnedProject(memberId, projectId);
        AiAnalysisJob latest = aiAnalysisJobRepository
                .findFirstByProject_IdOrderByCreatedAtDesc(projectId)
                .orElse(null);
        return toDashboard(projectId, latest);
    }

    private AiDashboardResponse toDashboard(Long projectId, AiAnalysisJob job) {
        String mode = photoSceneAnalyzer.isLiveGemini() ? "gemini" : "mock";
        double minConfidence = geminiProperties.getMinConfidence();
        if (job == null) {
            return new AiDashboardResponse(null, List.of(), List.of(), mode, minConfidence, 0);
        }
        List<AiPhotoResultResponse> all = aiPhotoResultRepository
                .findByJob_IdOrderBySceneCategoryAscIdAsc(job.getId())
                .stream()
                .map(r -> toResultResponse(projectId, r))
                .toList();
        List<AiPhotoResultResponse> results = all.stream()
                .filter(this::meetsConfidence)
                .toList();
        int excludedCount = all.size() - results.size();
        List<AiPhotoResultResponse> bestShots = results.stream()
                .filter(AiPhotoResultResponse::bestShot)
                .toList();
        return new AiDashboardResponse(
                toJobResponse(job, mode), results, bestShots, mode, minConfidence, excludedCount);
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

    private boolean meetsConfidence(AiPhotoResultResponse result) {
        BigDecimal min = BigDecimal.valueOf(geminiProperties.getMinConfidence());
        return result.confidence() != null && result.confidence().compareTo(min) >= 0;
    }

    private byte[] readBytes(UploadFile file) {
        if (file.getStorageKey() == null) {
            return new byte[0];
        }
        try (InputStream in = objectStorage.open(file.getStorageKey())) {
            return in.readAllBytes();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private AiJobResponse toJobResponse(AiAnalysisJob job, String mode) {
        return new AiJobResponse(
                job.getId(),
                job.getStatus(),
                job.getTotalFiles(),
                job.getProcessedFiles(),
                job.getErrorMessage(),
                mode,
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getCreatedAt()
        );
    }

    private String buildMetadataJson(PhotoSceneAnalyzer.Analysis analysis) throws Exception {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", analysis.provider());
        metadata.put("note", analysis.note() == null ? "" : analysis.note());
        metadata.put("people", analysis.people() == null ? List.of() : analysis.people());
        metadata.put("objects", analysis.objects() == null ? List.of() : analysis.objects());
        metadata.put("place", analysis.place() == null ? "" : analysis.place());
        return objectMapper.writeValueAsString(metadata);
    }

    private AiPhotoResultResponse toResultResponse(Long projectId, AiPhotoResult result) {
        UploadFile file = result.getUploadFile();
        MetadataView metadata = parseMetadata(result.getMetadataJson());
        return new AiPhotoResultResponse(
                result.getId(),
                file.getId(),
                file.getOriginalFilename(),
                file.getGuestName(),
                "/api/projects/" + projectId + "/uploads/" + file.getId() + "/content",
                result.getSceneCategory(),
                result.isBestShot(),
                result.getConfidence(),
                metadata.people(),
                metadata.objects(),
                metadata.place(),
                result.getAnalyzedAt()
        );
    }

    private MetadataView parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return new MetadataView(List.of(), List.of(), "");
        }
        try {
            JsonNode node = objectMapper.readTree(metadataJson);
            return new MetadataView(
                    readStringList(node.path("people")),
                    readStringList(node.path("objects")),
                    node.path("place").asText(""));
        } catch (Exception e) {
            return new MetadataView(List.of(), List.of(), "");
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

    private record MetadataView(List<String> people, List<String> objects, String place) {
    }

    private WeddingProject getOwnedProject(Long memberId, Long projectId) {
        WeddingProject project = weddingProjectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new NotFoundException("Wedding Project not found"));
        if (!project.getOwner().getId().equals(memberId)) {
            throw new ForbiddenException("이 Wedding Project에 접근할 권한이 없습니다.");
        }
        return project;
    }
}
