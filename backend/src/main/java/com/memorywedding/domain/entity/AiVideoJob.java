package com.memorywedding.domain.entity;

import com.memorywedding.domain.enums.AiJobStatus;
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
@Table(name = "ai_video_job")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiVideoJob extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private WeddingProject project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private Member requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiJobStatus status;

    @Column(length = 100)
    private String driveFileId;

    @Column(length = 500)
    private String storageKey;

    @Column(length = 50)
    private String storageProvider;

    private Long fileSize;

    @Column(nullable = false)
    private int clipCount;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Builder
    public AiVideoJob(WeddingProject project, Member requestedBy) {
        this.project = project;
        this.requestedBy = requestedBy;
        this.status = AiJobStatus.PENDING;
        this.clipCount = 0;
    }

    public void markProcessing(int clipCount) {
        this.status = AiJobStatus.PROCESSING;
        this.clipCount = clipCount;
        this.startedAt = LocalDateTime.now();
        this.errorMessage = null;
        this.driveFileId = null;
        this.storageKey = null;
        this.storageProvider = null;
        this.fileSize = null;
    }

    public void markCompleted(String storageProvider, String storageKey, long fileSize) {
        this.status = AiJobStatus.COMPLETED;
        this.storageProvider = storageProvider;
        this.storageKey = storageKey;
        this.fileSize = fileSize;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String message) {
        this.status = AiJobStatus.FAILED;
        this.errorMessage = message;
        this.completedAt = LocalDateTime.now();
    }
}
