package com.memorywedding.project;

import com.memorywedding.common.BadRequestException;
import com.memorywedding.common.ForbiddenException;
import com.memorywedding.common.NotFoundException;
import com.memorywedding.domain.entity.Invitation;
import com.memorywedding.domain.entity.InviteLink;
import com.memorywedding.domain.entity.WeddingProject;
import com.memorywedding.domain.repository.InvitationRepository;
import com.memorywedding.domain.repository.InviteLinkRepository;
import com.memorywedding.domain.repository.WeddingProjectRepository;
import com.memorywedding.project.dto.ProjectBgmResponse;
import com.memorywedding.storage.ObjectStorage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectBgmService {

    private static final long MAX_BYTES = 15L * 1024 * 1024;
    private static final Set<String> AUDIO_TYPES = Set.of(
            "audio/mpeg",
            "audio/mp3",
            "audio/mp4",
            "audio/aac",
            "audio/wav",
            "audio/x-wav",
            "audio/x-m4a",
            "audio/m4a"
    );

    private final WeddingProjectRepository weddingProjectRepository;
    private final InviteLinkRepository inviteLinkRepository;
    private final InvitationRepository invitationRepository;
    private final ObjectStorage objectStorage;

    public ProjectBgmResponse getForOwner(Long memberId, Long projectId) {
        WeddingProject project = getOwnedProject(memberId, projectId);
        return toResponse(project, projectId, false, null);
    }

    @Transactional
    public ProjectBgmResponse upload(Long memberId, Long projectId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("음원 파일을 선택해 주세요.");
        }
        WeddingProject project = getOwnedProject(memberId, projectId);
        String mime = normalizeMime(file.getContentType(), file.getOriginalFilename());
        if (!AUDIO_TYPES.contains(mime)) {
            throw new BadRequestException("지원 형식: MP3, M4A, AAC, WAV");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("BGM은 15MB 이하만 업로드할 수 있습니다.");
        }

        String filename = safeFilename(file.getOriginalFilename());
        String objectKey = project.getSlug()
                + "/bgm/"
                + UUID.randomUUID().toString().replace("-", "")
                + "_"
                + filename;

        String previousKey = project.getBgmStorageKey();
        try (InputStream in = file.getInputStream()) {
            var stored = objectStorage.store(objectKey, in, file.getSize(), mime);
            project.applyBgm(stored.provider(), stored.objectKey(), filename, mime, file.getSize());
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("BGM 업로드에 실패했습니다.");
        }

        if (previousKey != null && !previousKey.equals(project.getBgmStorageKey())) {
            try {
                objectStorage.delete(previousKey);
            } catch (Exception ignored) {
                // best-effort
            }
        }
        return toResponse(project, projectId, false, null);
    }

    @Transactional
    public void delete(Long memberId, Long projectId) {
        WeddingProject project = getOwnedProject(memberId, projectId);
        String key = project.getBgmStorageKey();
        project.clearBgm();
        if (key != null && !key.isBlank()) {
            try {
                objectStorage.delete(key);
            } catch (Exception ignored) {
                // best-effort
            }
        }
    }

    public ResponseEntity<InputStreamResource> openForOwner(Long memberId, Long projectId) {
        WeddingProject project = getOwnedProject(memberId, projectId);
        return openStream(project);
    }

    public ResponseEntity<InputStreamResource> openPublic(String slug) {
        WeddingProject project = weddingProjectRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new NotFoundException("청첩장을 찾을 수 없습니다."));
        InviteLink link = inviteLinkRepository.findByProject_Id(project.getId())
                .orElseThrow(() -> new NotFoundException("청첩장을 찾을 수 없습니다."));
        if (!link.isActive()) {
            throw new ForbiddenException("이 청첩장 링크는 비활성화되어 있습니다.");
        }
        Invitation invitation = invitationRepository.findByProject_Id(project.getId())
                .orElseThrow(() -> new NotFoundException("청첩장을 찾을 수 없습니다."));
        if (!invitation.isPublished()) {
            throw new ForbiddenException("아직 공개되지 않은 청첩장입니다.");
        }
        if (!project.hasBgm()) {
            throw new NotFoundException("등록된 BGM이 없습니다.");
        }
        return openStream(project);
    }

    public Path copyToWorkDir(Long projectId, Path workDir) {
        WeddingProject project = weddingProjectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElse(null);
        if (project == null || !project.hasBgm()) {
            return null;
        }
        try (InputStream in = objectStorage.open(project.getBgmStorageKey())) {
            String ext = extension(project.getBgmOriginalFilename(), project.getBgmMimeType());
            Path target = workDir.resolve("project-bgm." + ext);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return Files.size(target) > 0 ? target : null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasBgm(Long projectId) {
        return weddingProjectRepository.findByIdAndDeletedAtIsNull(projectId)
                .map(WeddingProject::hasBgm)
                .orElse(false);
    }

    public ProjectBgmResponse toResponse(
            WeddingProject project, Long projectId, boolean publicPath, String slug) {
        if (project == null || !project.hasBgm()) {
            return ProjectBgmResponse.empty();
        }
        String path = publicPath
                ? "/api/public/w/" + slug + "/bgm/content"
                : "/api/projects/" + projectId + "/bgm/content";
        return new ProjectBgmResponse(
                true,
                project.getBgmOriginalFilename(),
                project.getBgmMimeType(),
                project.getBgmFileSize(),
                path);
    }

    private ResponseEntity<InputStreamResource> openStream(WeddingProject project) {
        if (!project.hasBgm()) {
            throw new NotFoundException("등록된 BGM이 없습니다.");
        }
        InputStream stream = objectStorage.open(project.getBgmStorageKey());
        String mime = project.getBgmMimeType() == null ? "audio/mpeg" : project.getBgmMimeType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mime))
                .header("Cache-Control", "public, max-age=3600")
                .body(new InputStreamResource(stream));
    }

    private WeddingProject getOwnedProject(Long memberId, Long projectId) {
        WeddingProject project = weddingProjectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new NotFoundException("Wedding Project not found"));
        if (!project.getOwner().getId().equals(memberId)) {
            throw new ForbiddenException("이 Wedding Project에 접근할 권한이 없습니다.");
        }
        return project;
    }

    private static String normalizeMime(String contentType, String filename) {
        if (contentType != null && !contentType.isBlank()) {
            String mime = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
            if (AUDIO_TYPES.contains(mime)) {
                return mime.equals("audio/mp3") ? "audio/mpeg" : mime;
            }
        }
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (name.endsWith(".mp3")) {
            return "audio/mpeg";
        }
        if (name.endsWith(".m4a") || name.endsWith(".mp4")) {
            return "audio/mp4";
        }
        if (name.endsWith(".aac")) {
            return "audio/aac";
        }
        if (name.endsWith(".wav")) {
            return "audio/wav";
        }
        return contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
    }

    private static String safeFilename(String original) {
        if (original == null || original.isBlank()) {
            return "bgm.mp3";
        }
        String name = original.replaceAll("[\\\\/]+", "_").trim();
        return name.length() > 180 ? name.substring(name.length() - 180) : name;
    }

    private static String extension(String filename, String mime) {
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot >= 0 && dot < filename.length() - 1) {
                return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
            }
        }
        if (mime != null) {
            return switch (mime.toLowerCase(Locale.ROOT)) {
                case "audio/wav", "audio/x-wav" -> "wav";
                case "audio/mp4", "audio/x-m4a", "audio/m4a" -> "m4a";
                case "audio/aac" -> "aac";
                default -> "mp3";
            };
        }
        return "mp3";
    }
}
