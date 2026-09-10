package com.memorywedding.domain.entity;

import com.memorywedding.domain.enums.ProjectStatus;
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
@Table(name = "wedding_project")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeddingProject extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    @Column(nullable = false, length = 50)
    private String groomName;

    @Column(nullable = false, length = 50)
    private String brideName;

    @Column(nullable = false)
    private LocalDateTime weddingAt;

    @Column(length = 200)
    private String venueName;

    @Column(length = 500)
    private String venueAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status;

    @Column(length = 100)
    private String driveRootFolderId;

    private LocalDateTime deletedAt;

    @Builder
    public WeddingProject(
            Member owner,
            String slug,
            String groomName,
            String brideName,
            LocalDateTime weddingAt,
            String venueName,
            String venueAddress,
            ProjectStatus status) {
        this.owner = owner;
        this.slug = slug;
        this.groomName = groomName;
        this.brideName = brideName;
        this.weddingAt = weddingAt;
        this.venueName = venueName;
        this.venueAddress = venueAddress;
        this.status = status;
    }

    public void update(
            String groomName,
            String brideName,
            LocalDateTime weddingAt,
            String venueName,
            String venueAddress,
            ProjectStatus status) {
        this.groomName = groomName;
        this.brideName = brideName;
        this.weddingAt = weddingAt;
        this.venueName = venueName;
        this.venueAddress = venueAddress;
        if (status != null) {
            this.status = status;
        }
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.status = ProjectStatus.ARCHIVED;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
