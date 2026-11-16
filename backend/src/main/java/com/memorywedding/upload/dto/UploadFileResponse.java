package com.memorywedding.upload.dto;

import com.memorywedding.domain.entity.UploadFile;
import com.memorywedding.domain.enums.FileType;
import com.memorywedding.domain.enums.UploadStatus;
import java.time.LocalDateTime;

public record UploadFileResponse(
        Long id,
        FileType fileType,
        String guestName,
        String originalFilename,
        String mimeType,
        long fileSize,
        UploadStatus uploadStatus,
        String failureReason,
        boolean driveSynced,
        LocalDateTime createdAt
) {
    public static UploadFileResponse from(UploadFile file) {
        return new UploadFileResponse(
                file.getId(),
                file.getFileType(),
                file.getGuestName(),
                file.getOriginalFilename(),
                file.getMimeType(),
                file.getFileSize(),
                file.getUploadStatus(),
                file.getFailureReason(),
                file.getDriveFileId() != null && !file.getDriveFileId().isBlank(),
                file.getCreatedAt()
        );
    }
}
