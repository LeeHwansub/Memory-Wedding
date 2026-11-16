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
@Table(name = "ai_analysis_job")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiAnalysisJob extends BaseTimeEntity {

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

    @Column(nullable = false)
    private int totalFiles;

    @Column(nullable = false)
    private int processedFiles;

    @Column(length = 500)
    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Builder
    public AiAnalysisJob(WeddingProject project, Member requestedBy) {
        this.project = project;
        this.requestedBy = requestedBy;
        this.status = AiJobStatus.PENDING;
        this.totalFiles = 0;
        this.processedFiles = 0;
    }

    public void markProcessing(int totalFiles) {
        this.status = AiJobStatus.PROCESSING;
        this.totalFiles = totalFiles;
        this.processedFiles = 0;
        this.startedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void incrementProcessed() {
        this.processedFiles += 1;
    }

    public void markCompleted() {
        this.status = AiJobStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String message) {
        this.status = AiJobStatus.FAILED;
        this.errorMessage = message;
        this.completedAt = LocalDateTime.now();
    }
}
