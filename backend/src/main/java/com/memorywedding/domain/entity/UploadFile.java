package com.memorywedding.domain.entity;

import com.memorywedding.domain.enums.FileType;
import com.memorywedding.domain.enums.UploadStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "upload_file")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UploadFile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private WeddingProject project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileType fileType;

    @Column(nullable = false, length = 50)
    private String guestName;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 100)
    private String mimeType;

    @Column(nullable = false)
    private long fileSize;

    /** Our backup object key (local path relative or GCS object name). */
    @Column(length = 1000)
    private String storageKey;

    @Column(length = 20)
    private String storageProvider;

    @Column(length = 100)
    private String driveFileId;

    @Column(length = 500)
    private String driveFolderPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UploadStatus uploadStatus;

    @Column(length = 500)
    private String failureReason;

    private LocalDateTime deletedAt;

    @Builder
    public UploadFile(
            WeddingProject project,
            FileType fileType,
            String guestName,
            String originalFilename,
            String mimeType,
            long fileSize) {
        this.project = project;
        this.fileType = fileType;
        this.guestName = guestName;
        this.originalFilename = originalFilename;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.uploadStatus = UploadStatus.PENDING;
    }

    public void markUploading() {
        this.uploadStatus = UploadStatus.UPLOADING;
        this.failureReason = null;
    }

    public void markCompleted(String storageProvider, String storageKey) {
        this.storageProvider = storageProvider;
        this.storageKey = storageKey;
        this.uploadStatus = UploadStatus.COMPLETED;
        this.failureReason = null;
    }

    public void markFailed(String reason) {
        this.uploadStatus = UploadStatus.FAILED;
        this.failureReason = reason;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markDriveSynced(String driveFileId, String driveFolderPath) {
        this.driveFileId = driveFileId;
        this.driveFolderPath = driveFolderPath;
    }
}
