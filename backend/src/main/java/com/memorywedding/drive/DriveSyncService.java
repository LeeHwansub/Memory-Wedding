package com.memorywedding.drive;

import com.google.api.services.drive.Drive;
import com.memorywedding.common.BadRequestException;
import com.memorywedding.common.ForbiddenException;
import com.memorywedding.common.NotFoundException;
import com.memorywedding.domain.entity.AiVideoJob;
import com.memorywedding.domain.entity.DriveConnection;
import com.memorywedding.domain.entity.UploadFile;
import com.memorywedding.domain.entity.WeddingProject;
import com.memorywedding.domain.enums.FileType;
import com.memorywedding.domain.enums.UploadStatus;
import com.memorywedding.domain.repository.AiVideoJobRepository;
import com.memorywedding.domain.repository.DriveConnectionRepository;
import com.memorywedding.domain.repository.UploadFileRepository;
import com.memorywedding.domain.repository.WeddingProjectRepository;
import com.memorywedding.drive.dto.DriveSyncResultResponse;
import com.memorywedding.drive.dto.ProjectDriveStatusResponse;
import com.memorywedding.storage.ObjectStorage;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriveSyncService {

    private final DriveOAuthService driveOAuthService;
    private final DriveConnectionRepository driveConnectionRepository;
    private final WeddingProjectRepository weddingProjectRepository;
    private final UploadFileRepository uploadFileRepository;
    private final AiVideoJobRepository aiVideoJobRepository;
    private final ObjectStorage objectStorage;
    private final GoogleDriveClient googleDriveClient;

    public ProjectDriveStatusResponse projectStatus(Long memberId, Long projectId) {
        WeddingProject project = getOwnedProject(memberId, projectId);
        DriveConnection connection = driveConnectionRepository.findByMember_Id(memberId).orElse(null);
        boolean connected = connection != null;
        long pending = uploadFileRepository
                .countByProject_IdAndUploadStatusAndDriveFileIdIsNullAndDeletedAtIsNull(
                        project.getId(), UploadStatus.COMPLETED);
        long synced = uploadFileRepository
                .countByProject_IdAndUploadStatusAndDriveFileIdIsNotNullAndDeletedAtIsNull(
                        project.getId(), UploadStatus.COMPLETED);
        String path = project.getDriveRootFolderId() == null
                ? null
                : "Memory Wedding/Wedding_" + project.getSlug();
        return new ProjectDriveStatusResponse(
                connected,
                connection == null ? null : connection.getGoogleAccountEmail(),
                project.getDriveRootFolderId() != null && !project.getDriveRootFolderId().isBlank(),
                path,
                pending,
                synced
        );
    }

    /** Best-effort sync after guest upload. Never throws to caller. */
    @Transactional
    public void syncAfterUploadBestEffort(Long uploadFileId) {
        try {
            UploadFile file = uploadFileRepository.findById(uploadFileId).orElse(null);
            if (file == null || file.isDeleted() || file.getUploadStatus() != UploadStatus.COMPLETED) {
                return;
            }
            if (file.getDriveFileId() != null && !file.getDriveFileId().isBlank()) {
                return;
            }
            Long ownerId = file.getProject().getOwner().getId();
            if (!driveConnectionRepository.existsByMember_Id(ownerId)) {
                return;
            }
            syncFile(ownerId, file.getProject().getId(), file.getId());
        } catch (Exception e) {
            log.warn("Drive sync skipped for upload {}: {}", uploadFileId, e.getMessage());
        }
    }

    /** Best-effort: upload highlight MP4 to Drive AI/ and Archive/. Never fails highlight. */
    @Transactional
    public void syncHighlightBestEffort(Long videoJobId) {
        try {
            AiVideoJob job = aiVideoJobRepository.findById(videoJobId).orElse(null);
            if (job == null || job.getStorageKey() == null || job.getStorageKey().isBlank()) {
                return;
            }
            if (job.getDriveFileId() != null && !job.getDriveFileId().isBlank()) {
                return;
            }
            Long ownerId = job.getProject().getOwner().getId();
            if (!driveConnectionRepository.existsByMember_Id(ownerId)) {
                log.info("Drive not connected; skip highlight upload for job {}", videoJobId);
                return;
            }

            WeddingProject project = job.getProject();
            DriveConnection connection = driveOAuthService.requireConnection(ownerId);
            driveOAuthService.ensureAppRoot(connection);
            Drive drive = googleDriveClient.drive(driveOAuthService.decryptRefreshToken(connection));

            String projectFolderId = ensureProjectFolder(drive, connection, project);
            String aiFolderId = googleDriveClient.findOrCreateFolder(drive, "AI", projectFolderId);
            String archiveFolderId = googleDriveClient.findOrCreateFolder(drive, "Archive", projectFolderId);

            String filename = "highlight-" + project.getSlug() + "-" + job.getId() + ".mp4";
            long size = job.getFileSize() == null ? 0L : job.getFileSize();

            String aiFileId;
            try (InputStream in = objectStorage.open(job.getStorageKey())) {
                aiFileId = googleDriveClient.uploadFile(
                        drive, aiFolderId, filename, "video/mp4", in, size);
            }
            try (InputStream in = objectStorage.open(job.getStorageKey())) {
                googleDriveClient.uploadFile(
                        drive, archiveFolderId, filename, "video/mp4", in, size);
            }

            job.markDriveSynced(aiFileId);
            aiVideoJobRepository.save(job);
            log.info("Highlight job {} uploaded to Drive AI/ and Archive/ ({})", videoJobId, aiFileId);
        } catch (Exception e) {
            log.warn("Drive highlight sync skipped for job {}: {}", videoJobId, e.getMessage());
        }
    }

    @Transactional
    public void syncFile(Long memberId, Long projectId, Long fileId) {
        WeddingProject project = getOwnedProject(memberId, projectId);
        UploadFile file = uploadFileRepository
                .findByIdAndProject_IdAndDeletedAtIsNull(fileId, project.getId())
                .orElseThrow(() -> new NotFoundException("파일을 찾을 수 없습니다."));
        if (file.getUploadStatus() != UploadStatus.COMPLETED || file.getStorageKey() == null) {
            throw new BadRequestException("아직 Drive로 보낼 수 없는 파일입니다.");
        }
        if (file.getDriveFileId() != null && !file.getDriveFileId().isBlank()) {
            return;
        }

        DriveConnection connection = driveOAuthService.requireConnection(memberId);
        driveOAuthService.ensureAppRoot(connection);
        Drive drive = googleDriveClient.drive(driveOAuthService.decryptRefreshToken(connection));

        String projectFolderId = ensureProjectFolder(drive, connection, project);
        String typeFolderName = file.getFileType() == FileType.PHOTO ? "Photos" : "Videos";
        String typeFolderId = googleDriveClient.findOrCreateFolder(drive, typeFolderName, projectFolderId);
        String guestFolderId = googleDriveClient.findOrCreateFolder(drive, file.getGuestName(), typeFolderId);

        try (InputStream in = objectStorage.open(file.getStorageKey())) {
            String driveFileId = googleDriveClient.uploadFile(
                    drive,
                    guestFolderId,
                    file.getOriginalFilename(),
                    file.getMimeType(),
                    in,
                    file.getFileSize());
            String folderPath = "Memory Wedding/Wedding_" + project.getSlug()
                    + "/" + typeFolderName + "/" + file.getGuestName();
            file.markDriveSynced(driveFileId, folderPath);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Drive 사본 업로드에 실패했습니다.");
        }
    }

    @Transactional
    public DriveSyncResultResponse syncPending(Long memberId, Long projectId) {
        WeddingProject project = getOwnedProject(memberId, projectId);
        if (!driveConnectionRepository.existsByMember_Id(memberId)) {
            throw new BadRequestException("먼저 Google Drive를 연결해 주세요.");
        }
        List<UploadFile> pending = uploadFileRepository
                .findByProject_IdAndUploadStatusAndDriveFileIdIsNullAndDeletedAtIsNullOrderByCreatedAtAsc(
                        project.getId(), UploadStatus.COMPLETED);
        int synced = 0;
        int failed = 0;
        for (UploadFile file : pending) {
            try {
                syncFile(memberId, projectId, file.getId());
                synced++;
            } catch (Exception e) {
                log.warn("Drive sync failed for {}: {}", file.getId(), e.getMessage());
                failed++;
            }
        }
        return new DriveSyncResultResponse(synced, 0, failed);
    }

    @Transactional
    public void ensureProjectFolders(Long memberId, Long projectId) {
        WeddingProject project = getOwnedProject(memberId, projectId);
        DriveConnection connection = driveOAuthService.requireConnection(memberId);
        driveOAuthService.ensureAppRoot(connection);
        Drive drive = googleDriveClient.drive(driveOAuthService.decryptRefreshToken(connection));
        ensureProjectFolder(drive, connection, project);
        String projectFolderId = project.getDriveRootFolderId();
        googleDriveClient.findOrCreateFolder(drive, "Photos", projectFolderId);
        googleDriveClient.findOrCreateFolder(drive, "Videos", projectFolderId);
        googleDriveClient.findOrCreateFolder(drive, "AI", projectFolderId);
        googleDriveClient.findOrCreateFolder(drive, "Archive", projectFolderId);
    }

    private String ensureProjectFolder(Drive drive, DriveConnection connection, WeddingProject project) {
        if (project.getDriveRootFolderId() != null && !project.getDriveRootFolderId().isBlank()) {
            return project.getDriveRootFolderId();
        }
        String folderName = "Wedding_" + project.getSlug();
        String folderId = googleDriveClient.findOrCreateFolder(
                drive, folderName, connection.getDriveRootFolderId());
        project.assignDriveRootFolderId(folderId);
        return folderId;
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
