package com.memorywedding.domain.entity;

import com.memorywedding.domain.enums.SceneCategory;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ai_photo_result")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiPhotoResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private AiAnalysisJob job;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "upload_file_id", nullable = false, unique = true)
    private UploadFile uploadFile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SceneCategory sceneCategory;

    @Column(nullable = false)
    private boolean bestShot;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    private LocalDateTime analyzedAt;

    @Builder
    public AiPhotoResult(
            AiAnalysisJob job,
            UploadFile uploadFile,
            SceneCategory sceneCategory,
            boolean bestShot,
            BigDecimal confidence,
            String metadataJson) {
        this.job = job;
        this.uploadFile = uploadFile;
        this.sceneCategory = sceneCategory;
        this.bestShot = bestShot;
        this.confidence = confidence;
        this.metadataJson = metadataJson;
        this.analyzedAt = LocalDateTime.now();
    }

    public void updateAnalysis(
            SceneCategory sceneCategory,
            boolean bestShot,
            BigDecimal confidence,
            String metadataJson) {
        this.sceneCategory = sceneCategory;
        this.bestShot = bestShot;
        this.confidence = confidence;
        this.metadataJson = metadataJson;
        this.analyzedAt = LocalDateTime.now();
    }

    public void rebind(
            AiAnalysisJob job,
            SceneCategory sceneCategory,
            boolean bestShot,
            BigDecimal confidence,
            String metadataJson) {
        this.job = job;
        updateAnalysis(sceneCategory, bestShot, confidence, metadataJson);
    }

    public void clearBestShot() {
        this.bestShot = false;
    }

    public void markBestShot() {
        this.bestShot = true;
    }
}
