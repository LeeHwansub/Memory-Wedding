package com.memorywedding.domain.entity;

import com.memorywedding.domain.enums.InvitationMediaType;
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
@Table(name = "invitation_media")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvitationMedia extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitation_id", nullable = false)
    private Invitation invitation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationMediaType mediaType;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 100)
    private String mimeType;

    @Column(nullable = false)
    private long fileSize;

    @Column(length = 1000)
    private String storageKey;

    @Column(length = 20)
    private String storageProvider;

    @Column(nullable = false)
    private int sortOrder;

    private LocalDateTime deletedAt;

    @Builder
    public InvitationMedia(
            Invitation invitation,
            InvitationMediaType mediaType,
            String originalFilename,
            String mimeType,
            long fileSize,
            int sortOrder) {
        this.invitation = invitation;
        this.mediaType = mediaType;
        this.originalFilename = originalFilename;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.sortOrder = sortOrder;
    }

    public void markStored(String storageProvider, String storageKey) {
        this.storageProvider = storageProvider;
        this.storageKey = storageKey;
    }

    public void updateSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
