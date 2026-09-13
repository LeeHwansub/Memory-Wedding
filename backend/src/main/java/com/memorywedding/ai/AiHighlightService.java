package com.memorywedding.ai;

import com.memorywedding.ai.dto.AiDashboardResponse;
import com.memorywedding.ai.dto.AiVideoJobResponse;
import com.memorywedding.common.BadRequestException;
import com.memorywedding.common.ForbiddenException;
import com.memorywedding.common.NotFoundException;
import com.memorywedding.config.GeminiProperties;
import com.memorywedding.domain.entity.AiAnalysisJob;
import com.memorywedding.domain.entity.AiPhotoResult;
import com.memorywedding.domain.entity.AiVideoJob;
import com.memorywedding.domain.entity.Member;
import com.memorywedding.domain.entity.UploadFile;
import com.memorywedding.domain.entity.WeddingProject;
import com.memorywedding.domain.enums.AiJobStatus;
import com.memorywedding.domain.enums.FileType;
import com.memorywedding.domain.repository.AiAnalysisJobRepository;
import com.memorywedding.domain.repository.AiPhotoResultRepository;
import com.memorywedding.domain.repository.AiVideoJobRepository;
import com.memorywedding.domain.repository.MemberRepository;
import com.memorywedding.domain.repository.WeddingProjectRepository;
import com.memorywedding.storage.ObjectStorage;
import com.memorywedding.drive.DriveSyncService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiHighlightService {

    private static final double SECONDS_PER_IMAGE = 3.2;

    private final WeddingProjectRepository weddingProjectRepository;
    private final MemberRepository memberRepository;
    private final AiAnalysisJobRepository aiAnalysisJobRepository;
    private final AiPhotoResultRepository aiPhotoResultRepository;
    private final AiVideoJobRepository aiVideoJobRepository;
    private final ObjectStorage objectStorage;
    private final HighlightVideoComposer highlightVideoComposer;
    private final DriveSyncService driveSyncService;
    private final GeminiProperties geminiProperties;

    @Transactional
    public AiVideoJobResponse createHighlight(Long memberId, Long projectId) {
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

        AiAnalysisJob analysis = aiAnalysisJobRepository
                .findFirstByProject_IdOrderByCreatedAtDesc(projectId)
                .orElseThrow(() -> new BadRequestException("먼저 AI 분석을 실행해 주세요."));

        List<AiPhotoResult> bestPhotos = aiPhotoResultRepository
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
                .toList();

        if (bestPhotos.isEmpty()) {
            throw new BadRequestException(
                    "하이라이트에 쓸 Best Shot 사진이 없습니다. AI 분석 후 다시 시도해 주세요.");
        }

        AiVideoJob job = aiVideoJobRepository.save(
                AiVideoJob.builder().project(project).requestedBy(member).build());
        job.markProcessing(bestPhotos.size());

        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("mw-highlight-in-");
            List<Path> images = new ArrayList<>();
            int index = 0;
            for (AiPhotoResult result : bestPhotos) {
                UploadFile file = result.getUploadFile();
                byte[] bytes = readBytes(file);
                if (bytes.length == 0) {
                    continue;
                }
                String ext = guessExt(file.getOriginalFilename(), file.getMimeType());
                Path imagePath = workDir.resolve(String.format("img-%03d.%s", index, ext));
                Files.write(imagePath, bytes);
                images.add(imagePath);
                index += 1;
            }
            if (images.isEmpty()) {
                job.markFailed("Best Shot 원본을 읽지 못했습니다.");
                return toResponse(projectId, job);
            }

            byte[] mp4 = highlightVideoComposer.composeSlideshow(images, SECONDS_PER_IMAGE);
            String objectKey = "ai-highlight/" + projectId + "/" + UUID.randomUUID() + ".mp4";
            ObjectStorage.StoredObject stored = objectStorage.store(
                    objectKey,
                    new ByteArrayInputStream(mp4),
                    mp4.length,
                    "video/mp4");
            job.markCompleted(stored.provider(), stored.objectKey(), mp4.length);
        } catch (Exception e) {
            log.warn("Highlight video failed for project {}: {}", projectId, e.getMessage());
            job.markFailed(e.getMessage() == null ? "하이라이트 영상 생성 실패" : e.getMessage());
        } finally {
            if (workDir != null) {
                try (var walk = Files.walk(workDir)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                            // best-effort
                        }
                    });
                } catch (Exception ignored) {
                    // best-effort
                }
            }
        }

        AiVideoJob saved = aiVideoJobRepository.save(job);
        if (saved.getStatus() == AiJobStatus.COMPLETED) {
            driveSyncService.syncHighlightBestEffort(saved.getId());
        }
        return toResponse(
                projectId,
                aiVideoJobRepository.findById(saved.getId()).orElse(saved));
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
        // reload in case Drive sync updated driveFileId in another transaction
        AiVideoJob fresh = aiVideoJobRepository.findById(job.getId()).orElse(job);
        String contentPath = fresh.getStorageKey() != null && !fresh.getStorageKey().isBlank()
                ? "/api/projects/" + projectId + "/ai/video/" + fresh.getId() + "/content"
                : null;
        boolean driveSynced = fresh.getDriveFileId() != null && !fresh.getDriveFileId().isBlank();
        return new AiVideoJobResponse(
                fresh.getId(),
                fresh.getStatus(),
                fresh.getClipCount(),
                fresh.getFileSize(),
                contentPath,
                fresh.getDriveFileId(),
                driveSynced,
                fresh.getErrorMessage(),
                fresh.getStartedAt(),
                fresh.getCompletedAt(),
                fresh.getCreatedAt()
        );
    }

    private boolean meetsConfidence(AiPhotoResult result) {
        BigDecimal min = BigDecimal.valueOf(geminiProperties.getMinConfidence());
        return result.getConfidence() != null && result.getConfidence().compareTo(min) >= 0;
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

    private String guessExt(String filename, String mime) {
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot > 0) {
                String ext = filename.substring(dot + 1).toLowerCase();
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

    private WeddingProject getOwnedProject(Long memberId, Long projectId) {
        WeddingProject project = weddingProjectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new NotFoundException("Wedding Project not found"));
        if (!project.getOwner().getId().equals(memberId)) {
            throw new ForbiddenException("이 Wedding Project에 접근할 권한이 없습니다.");
        }
        return project;
    }
}
