package com.memorywedding.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorywedding.ai.dto.AiVideoJobResponse;
import com.memorywedding.ai.dto.CreateHighlightRequest;
import com.memorywedding.common.BadRequestException;
import com.memorywedding.common.ForbiddenException;
import com.memorywedding.common.NotFoundException;
import com.memorywedding.domain.entity.AiPhotoResult;
import com.memorywedding.domain.entity.AiVideoJob;
import com.memorywedding.domain.entity.Member;
import com.memorywedding.domain.entity.UploadFile;
import com.memorywedding.domain.entity.WeddingProject;
import com.memorywedding.domain.enums.AiJobStatus;
import com.memorywedding.domain.enums.FileType;
import com.memorywedding.domain.enums.SceneCategory;
import com.memorywedding.domain.repository.AiVideoJobRepository;
import com.memorywedding.domain.repository.MemberRepository;
import com.memorywedding.domain.repository.WeddingProjectRepository;
import com.memorywedding.drive.DriveSyncService;
import com.memorywedding.storage.ObjectStorage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiHighlightService {

    private final WeddingProjectRepository weddingProjectRepository;
    private final MemberRepository memberRepository;
    private final AiVideoJobRepository aiVideoJobRepository;
    private final ObjectStorage objectStorage;
    private final HighlightVideoComposer highlightVideoComposer;
    private final DriveSyncService driveSyncService;
    private final AiAsyncDispatcher aiAsyncDispatcher;
    private final AiJobProgressService aiJobProgressService;
    private final AiHighlightWriteService aiHighlightWriteService;
    private final ObjectMapper objectMapper;

    @Transactional
    public AiVideoJobResponse createHighlight(
            Long memberId, Long projectId, CreateHighlightRequest request) {
        WeddingProject project = getOwnedProject(memberId, projectId);
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("Member not found"));

        if (!highlightVideoComposer.isAvailable()) {
            throw new BadRequestException("FFmpeg가 없어 하이라이트 영상을 생성할 수 없습니다.");
        }

        if (aiVideoJobRepository.existsByProject_IdAndStatusIn(
                projectId, List.of(AiJobStatus.PENDING, AiJobStatus.PROCESSING))) {
            throw new BadRequestException("이미 하이라이트 영상을 생성 중입니다. 완료 후 다시 시도해 주세요.");
        }

        HighlightOptions options = resolveOptions(request);
        List<AiPhotoResult> assets = aiHighlightWriteService.loadHighlightAssets(
                projectId, options.maxClips());
        if (assets.isEmpty()) {
            throw new BadRequestException(
                    "하이라이트에 쓸 대표 컷 사진·영상이 없습니다. AI 분석 후 다시 시도해 주세요.");
        }

        AiVideoJob job = aiVideoJobRepository.save(
                AiVideoJob.builder().project(project).requestedBy(member).build());
        job.applyOptionsJson(toOptionsJson(options));
        job.markProcessing(assets.size());
        aiVideoJobRepository.save(job);

        Long jobId = job.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                aiAsyncDispatcher.runHighlight(jobId);
            }
        });

        return toResponse(projectId, job);
    }

    public void processHighlightJob(Long jobId) {
        Path workDir = null;
        try {
            Long projectId = aiHighlightWriteService.requireProjectId(jobId);
            HighlightOptions options = parseOptionsJson(
                    aiHighlightWriteService.requireOptionsJson(jobId));
            List<AiPhotoResult> assets = aiHighlightWriteService.loadHighlightAssets(
                    projectId, options.maxClips());
            if (assets.isEmpty()) {
                aiJobProgressService.markHighlightFailed(jobId, "대표 컷 사진·영상이 없습니다.");
                return;
            }

            workDir = Files.createTempDirectory("mw-highlight-in-");
            List<HighlightVideoComposer.Segment> segments = new ArrayList<>();
            int index = 0;
            for (AiPhotoResult result : assets) {
                UploadFile file = result.getUploadFile();
                FileType type = file.getFileType();
                boolean video = type == FileType.VIDEO;
                String ext = video
                        ? guessVideoExt(file.getOriginalFilename(), file.getMimeType())
                        : guessImageExt(file.getOriginalFilename(), file.getMimeType());
                Path mediaPath = workDir.resolve(String.format(
                        Locale.ROOT, "%s-%03d.%s", video ? "vid" : "img", index, ext));
                if (!copyToFile(file, mediaPath)) {
                    log.warn("Skip highlight asset upload {}: empty or unreadable", file.getId());
                    continue;
                }
                String sceneLabel = options.subtitles()
                        ? sceneLabelKo(result.getSceneCategory())
                        : null;
                segments.add(new HighlightVideoComposer.Segment(
                        video
                                ? HighlightVideoComposer.SegmentKind.VIDEO
                                : HighlightVideoComposer.SegmentKind.PHOTO,
                        mediaPath,
                        sceneLabel));
                index += 1;
                aiJobProgressService.bumpHighlightProcessed(jobId);
            }
            if (segments.isEmpty()) {
                aiJobProgressService.markHighlightFailed(jobId, "대표 컷 원본을 읽지 못했습니다.");
                return;
            }

            byte[] mp4 = highlightVideoComposer.composeSegments(segments, options);
            String objectKey = "ai-highlight/" + projectId + "/" + UUID.randomUUID() + ".mp4";
            ObjectStorage.StoredObject stored = objectStorage.store(
                    objectKey,
                    new ByteArrayInputStream(mp4),
                    mp4.length,
                    "video/mp4");
            aiJobProgressService.completeHighlight(
                    jobId, stored.provider(), stored.objectKey(), mp4.length);
            driveSyncService.syncHighlightBestEffort(jobId);
        } catch (Exception e) {
            log.warn("Highlight video job {} failed: {}", jobId, e.getMessage());
            aiJobProgressService.markHighlightFailed(
                    jobId, e.getMessage() == null ? "하이라이트 영상 생성 실패" : e.getMessage());
        } finally {
            if (workDir != null) {
                try (var walk = Files.walk(workDir)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                            // 정리 실패는 무시
                        }
                    });
                } catch (Exception ignored) {
                    // 정리 실패는 무시
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public AiVideoJobResponse getLatest(Long memberId, Long projectId) {
        getOwnedProject(memberId, projectId);
        return aiVideoJobRepository.findFirstByProject_IdOrderByCreatedAtDesc(projectId)
                .map(job -> toResponse(projectId, job))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public AiVideoJob getOwnedJob(Long memberId, Long projectId, Long jobId) {
        getOwnedProject(memberId, projectId);
        AiVideoJob job = aiVideoJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("하이라이트 Job을 찾을 수 없습니다."));
        if (!job.getProject().getId().equals(projectId)) {
            throw new NotFoundException("하이라이트 Job을 찾을 수 없습니다.");
        }
        return job;
    }

    public InputStream openContent(AiVideoJob job) {
        if (job.getStorageKey() == null || job.getStorageKey().isBlank()) {
            throw new NotFoundException("생성된 영상이 없습니다.");
        }
        return objectStorage.open(job.getStorageKey());
    }

    public AiVideoJobResponse toResponse(Long projectId, AiVideoJob job) {
        AiVideoJob fresh = aiVideoJobRepository.findById(job.getId()).orElse(job);
        String contentPath = fresh.getStorageKey() != null && !fresh.getStorageKey().isBlank()
                ? "/api/projects/" + projectId + "/ai/video/" + fresh.getId() + "/content"
                : null;
        boolean driveSynced = fresh.getDriveFileId() != null && !fresh.getDriveFileId().isBlank();
        HighlightOptions options = parseOptionsJson(fresh.getOptionsJson());
        return new AiVideoJobResponse(
                fresh.getId(),
                fresh.getStatus(),
                fresh.getClipCount(),
                fresh.getProcessedClips(),
                fresh.getFileSize(),
                contentPath,
                fresh.getDriveFileId(),
                driveSynced,
                fresh.getErrorMessage(),
                options.style().name(),
                options.length().name(),
                options.bgm(),
                options.subtitles(),
                fresh.getStartedAt(),
                fresh.getCompletedAt(),
                fresh.getCreatedAt()
        );
    }

    private HighlightOptions resolveOptions(CreateHighlightRequest request) {
        if (request == null) {
            return HighlightOptions.defaults();
        }
        return HighlightOptions.fromRequest(
                request.style(), request.length(), request.bgm(), request.subtitles());
    }

    private String toOptionsJson(HighlightOptions options) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("style", options.style().name());
            map.put("length", options.length().name());
            map.put("bgm", options.bgm());
            map.put("subtitles", options.subtitles());
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{\"style\":\"CLASSIC\",\"length\":\"MEDIUM\",\"bgm\":false,\"subtitles\":false}";
        }
    }

    private HighlightOptions parseOptionsJson(String json) {
        if (json == null || json.isBlank()) {
            return HighlightOptions.defaults();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            return HighlightOptions.fromRequest(
                    textOrNull(node, "style"),
                    textOrNull(node, "length"),
                    node.has("bgm") && node.get("bgm").asBoolean(false),
                    node.has("subtitles") && node.get("subtitles").asBoolean(false));
        } catch (Exception e) {
            return HighlightOptions.defaults();
        }
    }

    private String textOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private String sceneLabelKo(SceneCategory category) {
        if (category == null) {
            return null;
        }
        return switch (category) {
            case ENTRANCE -> "입장";
            case SONG -> "축가";
            case GROUP_PHOTO -> "단체사진";
            case RECEPTION -> "피로연";
            case OTHER -> "기타";
        };
    }

    private boolean copyToFile(UploadFile file, Path dest) {
        if (file.getStorageKey() == null) {
            return false;
        }
        try (InputStream in = objectStorage.open(file.getStorageKey());
                OutputStream out = Files.newOutputStream(dest)) {
            in.transferTo(out);
            return Files.size(dest) > 0;
        } catch (Exception e) {
            log.warn("Failed to copy upload {} to temp: {}", file.getId(), e.getMessage());
            return false;
        }
    }

    private String guessImageExt(String filename, String mime) {
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot > 0) {
                String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (ext.matches("jpe?g|png|webp")) {
                    return ext.equals("jpeg") ? "jpg" : ext;
                }
            }
        }
        if (mime != null && mime.contains("png")) {
            return "png";
        }
        return "jpg";
    }

    private String guessVideoExt(String filename, String mime) {
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot > 0) {
                String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (ext.matches("mp4|mov|webm|m4v")) {
                    return ext;
                }
            }
        }
        if (mime != null) {
            if (mime.contains("webm")) {
                return "webm";
            }
            if (mime.contains("quicktime")) {
                return "mov";
            }
        }
        return "mp4";
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
