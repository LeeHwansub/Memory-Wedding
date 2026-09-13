package com.memorywedding.domain.entity;

import com.memorywedding.domain.enums.GalleryLayout;
import com.memorywedding.domain.enums.InvitationTemplate;
import com.memorywedding.domain.enums.MainPhotoPlacement;
import com.memorywedding.domain.enums.MediaDisplaySize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "invitation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private WeddingProject project;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String greetingMessage;

    @Column(nullable = false)
    private boolean published;

    @Column(length = 1000)
    private String mapUrl;

    @Column(columnDefinition = "TEXT")
    private String accountInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationTemplate template = InvitationTemplate.CLASSIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GalleryLayout galleryLayout = GalleryLayout.SLIDER;

    @Column(nullable = false)
    private int galleryColumns = 2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MediaDisplaySize galleryImageSize = MediaDisplaySize.MD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MediaDisplaySize mainPhotoSize = MediaDisplaySize.LG;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MainPhotoPlacement mainPhotoPlacement = MainPhotoPlacement.TOP;

    @Column(nullable = false)
    private double mainBrightness = 1.0;

    @Column(nullable = false)
    private double mainSaturation = 1.0;

    @Column(nullable = false)
    private int mainFocalX = 50;

    @Column(nullable = false)
    private int mainFocalY = 50;

    @Builder
    public Invitation(WeddingProject project, String title, String greetingMessage) {
        this.project = project;
        this.title = title;
        this.greetingMessage = greetingMessage;
        this.published = false;
        this.template = InvitationTemplate.CLASSIC;
        this.galleryLayout = GalleryLayout.SLIDER;
        this.galleryColumns = 2;
        this.galleryImageSize = MediaDisplaySize.MD;
        this.mainPhotoSize = MediaDisplaySize.LG;
        this.mainPhotoPlacement = MainPhotoPlacement.TOP;
        this.mainBrightness = 1.0;
        this.mainSaturation = 1.0;
        this.mainFocalX = 50;
        this.mainFocalY = 50;
    }

    public void updateContent(String title, String greetingMessage, String mapUrl, String accountInfo) {
        this.title = title;
        this.greetingMessage = greetingMessage;
        this.mapUrl = mapUrl;
        this.accountInfo = accountInfo;
    }

    public void updateMediaSettings(
            InvitationTemplate template,
            GalleryLayout galleryLayout,
            Integer galleryColumns,
            MediaDisplaySize galleryImageSize,
            MediaDisplaySize mainPhotoSize,
            MainPhotoPlacement mainPhotoPlacement,
            Double mainBrightness,
            Double mainSaturation,
            Integer mainFocalX,
            Integer mainFocalY) {
        if (template != null) {
            this.template = template;
        }
        if (galleryLayout != null) {
            this.galleryLayout = galleryLayout;
        }
        if (galleryColumns != null) {
            this.galleryColumns = Math.min(4, Math.max(2, galleryColumns));
        }
        if (galleryImageSize != null) {
            this.galleryImageSize = galleryImageSize;
        }
        if (mainPhotoSize != null) {
            this.mainPhotoSize = mainPhotoSize;
        }
        if (mainPhotoPlacement != null) {
            this.mainPhotoPlacement = mainPhotoPlacement;
        }
        if (mainBrightness != null) {
            this.mainBrightness = clamp(mainBrightness, 0.5, 1.5);
        }
        if (mainSaturation != null) {
            this.mainSaturation = clamp(mainSaturation, 0.5, 1.5);
        }
        if (mainFocalX != null) {
            this.mainFocalX = Math.min(100, Math.max(0, mainFocalX));
        }
        if (mainFocalY != null) {
            this.mainFocalY = Math.min(100, Math.max(0, mainFocalY));
        }
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    private double clamp(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
    }
}
