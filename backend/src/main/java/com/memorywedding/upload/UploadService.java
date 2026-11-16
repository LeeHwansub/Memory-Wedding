package com.memorywedding.upload;

import com.memorywedding.common.BadRequestException;
import com.memorywedding.common.ForbiddenException;
import com.memorywedding.common.NotFoundException;
import com.memorywedding.domain.entity.Invitation;
import com.memorywedding.domain.entity.InviteLink;
import com.memorywedding.domain.entity.UploadFile;
import com.memorywedding.domain.entity.WeddingProject;
import com.memorywedding.domain.enums.FileType;
import com.memorywedding.domain.enums.UploadStatus;
import com.memorywedding.domain.repository.InvitationRepository;
import com.memorywedding.domain.repository.InviteLinkRepository;
import com.memorywedding.domain.repository.UploadFileRepository;
import com.memorywedding.domain.repository.WeddingProjectRepository;
import com.memorywedding.drive.DriveSyncService;
import com.memorywedding.storage.ObjectStorage;
import com.memorywedding.upload.dto.UploadFileResponse;
import com.memorywedding.upload.dto.UploadFolderTreeResponse;
import com.memorywedding.upload.dto.UploadGuestFolderResponse;
import com.memorywedding.upload.dto.UploadPageResponse;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UploadService {

    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(12, 24, 48);
    private static final long MAX_PHOTO_BYTES = 30L * 1024 * 1024;
    private static final long MAX_VIDEO_BYTES = 300L * 1024 * 1024;
    private static final Set<String> PHOTO_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif"
    );
    private static final Set<String> VIDEO_TYPES = Set.of(
            "video/mp4", "video/quicktime", "video/webm"
    );

    private final UploadFileRepository uploadFileRepository;
    private final WeddingProjectRepository weddingProjectRepository;
    private final InviteLinkRepository inviteLinkRepository;
    private final InvitationRepository invitationRepository;
    private final ObjectStorage objectStorage;
    private final DriveSyncService driveSyncService;

    @Transactional
    public UploadFileResponse uploadPublic(String slug, String guestName, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("파일을 선택해 주세요.");
        }
        String name = guestName == null ? "" : guestName.trim();
        if (name.isBlank()) {
            throw new BadRequestException("이름을 입력해 주세요.");
        }
        if (name.length() > 50) {
            throw new BadRequestException("이름은 50자 이하여야 합니다.");
        }

        WeddingProject project = resolvePublicProject(slug);
        String mimeType = normalizeMime(file.getContentType(), file.getOriginalFilename());
        FileType fileType = resolveFileType(mimeType);
        long size = file.getSize();
        validateSize(fileType, size);

        String originalFilename = safeFilename(file.getOriginalFilename());
        UploadFile entity = uploadFileRepository.save(UploadFile.builder()
                .project(project)
                .fileType(fileType)
                .guestName(name)
                .originalFilename(originalFilename)
                .mimeType(mimeType)
                .fileSize(size)
                .build());

        entity.markUploading();
        String objectKey = buildObjectKey(project.getSlug(), fileType, name, originalFilename);

        try (InputStream in = file.getInputStream()) {
            var stored = objectStorage.store(objectKey, in, size, mimeType);
            entity.markCompleted(stored.provider(), stored.objectKey());
        } catch (Exception e) {
            throw new BadRequestException("파일 업로드에 실패했습니다.");
        }

        driveSyncService.syncAfterUploadBestEffort(entity.getId());
        UploadFile latest = uploadFileRepository.findById(entity.getId()).orElse(entity);
        return UploadFileResponse.from(latest);
    }

    public UploadPageResponse listPublic(String slug, int page, int size) {
        WeddingProject project = resolvePublicProject(slug);
        return toPage(project.getId(), page, size, UploadStatus.COMPLETED);
    }

    public UploadPageResponse listForOwner(
            Long memberId,
            Long projectId,
            int page,
            int size,
            FileType fileType,
            String guestName) {
        WeddingProject project = getOwnedProject(memberId, projectId);
        if (fileType != null && guestName != null && !guestName.isBlank()) {
            return toGuestPage(project.getId(), fileType, guestName.trim(), page, size);
        }
        return toPage(project.getId(), page, size, null);
    }

    public UploadFolderTreeResponse folderTreeForOwner(Long memberId, Long projectId) {
        WeddingProject project = getOwnedProject(memberId, projectId);
        long photoCount = uploadFileRepository.countByProject_IdAndFileTypeAndDeletedAtIsNull(
                project.getId(), FileType.PHOTO);
        long videoCount = uploadFileRepository.countByProject_IdAndFileTypeAndDeletedAtIsNull(
                project.getId(), FileType.VIDEO);
        return new UploadFolderTreeResponse(
                photoCount,
                videoCount,
                toGuestFolders(project.getId(), FileType.PHOTO),
                toGuestFolders(project.getId(), FileType.VIDEO)
        );
    }

    @Transactional
    public void deleteForOwner(Long memberId, Long projectId, Long fileId) {
        WeddingProject project = getOwnedProject(memberId, projectId);
        UploadFile file = uploadFileRepository
                .findByIdAndProject_IdAndDeletedAtIsNull(fileId, project.getId())
                .orElseThrow(() -> new NotFoundException("파일을 찾을 수 없습니다."));
        if (file.getStorageKey() != null) {
            objectStorage.delete(file.getStorageKey());
        }
        file.softDelete();
    }

    public ResponseEntity<InputStreamResource> openForOwner(Long memberId, Long projectId, Long fileId) {
        WeddingProject project = getOwnedProject(memberId, projectId);
        UploadFile file = uploadFileRepository
                .findByIdAndProject_IdAndDeletedAtIsNull(fileId, project.getId())
                .orElseThrow(() -> new NotFoundException("파일을 찾을 수 없습니다."));
        if (file.getUploadStatus() != UploadStatus.COMPLETED || file.getStorageKey() == null) {
            throw new BadRequestException("아직 열 수 없는 파일입니다.");
        }
        InputStream stream = objectStorage.open(file.getStorageKey());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getMimeType()))
                .header("Content-Disposition", "inline; filename=\"" + file.getOriginalFilename() + "\"")
                .body(new InputStreamResource(stream));
    }

    private UploadPageResponse toPage(Long projectId, int page, int size, UploadStatus statusFilter) {
        int safePage = Math.max(page, 0);
        int safeSize = ALLOWED_PAGE_SIZES.contains(size) ? size : 24;
        Page<UploadFile> result = statusFilter == null
                ? uploadFileRepository.findByProject_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        projectId, PageRequest.of(safePage, safeSize))
                : uploadFileRepository.findByProject_IdAndUploadStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                        projectId, statusFilter, PageRequest.of(safePage, safeSize));
        return toPageResponse(result);
    }

    private UploadPageResponse toGuestPage(
            Long projectId,
            FileType fileType,
            String guestName,
            int page,
            int size) {
        int safePage = Math.max(page, 0);
        int safeSize = ALLOWED_PAGE_SIZES.contains(size) ? size : 24;
        Page<UploadFile> result =
                uploadFileRepository.findByProject_IdAndFileTypeAndGuestNameAndDeletedAtIsNullOrderByCreatedAtDesc(
                        projectId, fileType, guestName, PageRequest.of(safePage, safeSize));
        return toPageResponse(result);
    }

    private UploadPageResponse toPageResponse(Page<UploadFile> result) {
        return new UploadPageResponse(
                result.getContent().stream().map(UploadFileResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private List<UploadGuestFolderResponse> toGuestFolders(Long projectId, FileType fileType) {
        return uploadFileRepository.countByGuestName(projectId, fileType).stream()
                .map(row -> new UploadGuestFolderResponse(
                        (String) row[0],
                        ((Number) row[1]).longValue()))
                .toList();
    }

    private WeddingProject resolvePublicProject(String slug) {
        WeddingProject project = weddingProjectRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new NotFoundException("업로드를 할 수 없습니다."));

        InviteLink link = inviteLinkRepository.findByProject_Id(project.getId())
                .orElseThrow(() -> new NotFoundException("업로드를 할 수 없습니다."));
        if (!link.isActive()) {
            throw new ForbiddenException("이 초대 링크는 비활성화되어 있습니다.");
        }

        Invitation invitation = invitationRepository.findByProject_Id(project.getId())
                .orElseThrow(() -> new NotFoundException("업로드를 할 수 없습니다."));
        if (!invitation.isPublished()) {
            throw new ForbiddenException("아직 공개되지 않은 청첩장입니다.");
        }

        return project;
    }

    private WeddingProject getOwnedProject(Long memberId, Long projectId) {
        WeddingProject project = weddingProjectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new NotFoundException("Wedding Project not found"));
        if (!project.getOwner().getId().equals(memberId)) {
            throw new ForbiddenException("이 Wedding Project에 접근할 권한이 없습니다.");
        }
        return project;
    }

    private FileType resolveFileType(String mimeType) {
        if (PHOTO_TYPES.contains(mimeType)) {
            return FileType.PHOTO;
        }
        if (VIDEO_TYPES.contains(mimeType)) {
            return FileType.VIDEO;
        }
        throw new BadRequestException("지원하지 않는 파일 형식입니다. (사진: JPG/PNG/WEBP/HEIC, 영상: MP4/MOV/WEBM)");
    }

    private void validateSize(FileType fileType, long size) {
        if (fileType == FileType.PHOTO && size > MAX_PHOTO_BYTES) {
            throw new BadRequestException("사진은 30MB 이하만 업로드할 수 있습니다.");
        }
        if (fileType == FileType.VIDEO && size > MAX_VIDEO_BYTES) {
            throw new BadRequestException("영상은 300MB 이하만 업로드할 수 있습니다.");
        }
    }

    private String normalizeMime(String contentType, String filename) {
        if (contentType != null && !contentType.isBlank() && !"application/octet-stream".equals(contentType)) {
            return contentType.toLowerCase(Locale.ROOT);
        }
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".heic")) return "image/heic";
        if (lower.endsWith(".heif")) return "image/heif";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".mov")) return "video/quicktime";
        if (lower.endsWith(".webm")) return "video/webm";
        throw new BadRequestException("파일 형식을 확인할 수 없습니다.");
    }

    private String safeFilename(String original) {
        if (original == null || original.isBlank()) {
            return "upload.bin";
        }
        String cleaned = original.replace("\\", "_").replace("/", "_").trim();
        return cleaned.length() > 200 ? cleaned.substring(cleaned.length() - 200) : cleaned;
    }

    private String buildObjectKey(String slug, FileType type, String guestName, String filename) {
        String safeGuest = guestName.replaceAll("[^a-zA-Z0-9가-힣_-]", "_");
        return slug + "/" + type.name().toLowerCase(Locale.ROOT) + "/" + safeGuest + "/"
                + UUID.randomUUID().toString().replace("-", "") + "_" + filename;
    }
}
