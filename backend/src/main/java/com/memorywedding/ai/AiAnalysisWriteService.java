package com.memorywedding.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisWriteService {

    private final AiAnalysisJobRepository aiAnalysisJobRepository;
    private final AiPhotoResultRepository aiPhotoResultRepository;
    private final UploadFileRepository uploadFileRepository;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

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
        markNearDuplicates(results);
        markBestShots(results);
        results.forEach(aiPhotoResultRepository::save);

        if (job.getProcessedFiles() == 0) {
            job.markFailed("분석에 성공한 사진·영상이 없습니다.");
        } else {
            job.markCompleted();
        }
        aiAnalysisJobRepository.save(job);
    }

    /**
     * PHOTO 간 aHash 유사도 기준으로 낮은 confidence 쪽을 중복으로 표시 (FR-AI-016).
     * VIDEO·해시 없음은 유지.
     */
    private void markNearDuplicates(List<AiPhotoResult> results) {
        List<AiPhotoResult> photos = results.stream()
                .filter(r -> r.getUploadFile().getFileType() == FileType.PHOTO)
                .filter(this::meetsConfidence)
                .sorted(Comparator.comparing(
                        (AiPhotoResult r) -> r.getConfidence() == null
                                ? BigDecimal.ZERO
                                : r.getConfidence()).reversed())
                .toList();

        Set<Long> suppressedIds = new HashSet<>();
        for (int i = 0; i < photos.size(); i++) {
            AiPhotoResult keep = photos.get(i);
            if (suppressedIds.contains(keep.getId())) {
                continue;
            }
            Long keepHash = readAhash(keep.getMetadataJson());
            if (keepHash == null) {
                writeDuplicateFlag(keep, false);
                continue;
            }
            writeDuplicateFlag(keep, false);
            for (int j = i + 1; j < photos.size(); j++) {
                AiPhotoResult other = photos.get(j);
                if (suppressedIds.contains(other.getId())) {
                    continue;
                }
                Long otherHash = readAhash(other.getMetadataJson());
                if (PhotoAverageHash.similar(keepHash, otherHash)) {
                    writeDuplicateFlag(other, true);
                    suppressedIds.add(other.getId());
                    log.debug("Near-duplicate photo {} suppressed by {}", other.getId(), keep.getId());
                }
            }
        }

        for (AiPhotoResult result : results) {
            if (result.getUploadFile().getFileType() != FileType.PHOTO) {
                writeDuplicateFlag(result, false);
            } else if (!meetsConfidence(result) && readDuplicateFlag(result.getMetadataJson()) == null) {
                writeDuplicateFlag(result, false);
            }
        }
    }

    private void markBestShots(List<AiPhotoResult> results) {
        Map<SceneCategory, List<AiPhotoResult>> byScene = new EnumMap<>(SceneCategory.class);
        for (AiPhotoResult result : results) {
            result.clearBestShot();
            if (!meetsConfidence(result) || isDuplicate(result)) {
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

    private boolean isDuplicate(AiPhotoResult result) {
        return Boolean.TRUE.equals(readDuplicateFlag(result.getMetadataJson()));
    }

    private Long readAhash(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(metadataJson);
            if (!node.has("ahash")) {
                return null;
            }
            return PhotoAverageHash.fromHex(node.get("ahash").asText());
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean readDuplicateFlag(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(metadataJson);
            if (!node.has("duplicate")) {
                return null;
            }
            return node.get("duplicate").asBoolean();
        } catch (Exception e) {
            return null;
        }
    }

    private void writeDuplicateFlag(AiPhotoResult result, boolean duplicate) {
        try {
            ObjectNode node;
            if (result.getMetadataJson() == null || result.getMetadataJson().isBlank()) {
                node = objectMapper.createObjectNode();
            } else {
                JsonNode parsed = objectMapper.readTree(result.getMetadataJson());
                node = parsed.isObject()
                        ? (ObjectNode) parsed
                        : objectMapper.createObjectNode();
            }
            node.put("duplicate", duplicate);
            result.replaceMetadataJson(objectMapper.writeValueAsString(node));
        } catch (Exception e) {
            log.warn("Failed to write duplicate flag for result {}: {}", result.getId(), e.getMessage());
        }
    }

    private boolean meetsConfidence(AiPhotoResult result) {
        BigDecimal min = BigDecimal.valueOf(geminiProperties.getMinConfidence());
        return result.getConfidence() != null && result.getConfidence().compareTo(min) >= 0;
    }
}
