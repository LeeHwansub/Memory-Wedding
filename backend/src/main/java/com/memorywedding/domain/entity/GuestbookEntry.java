package com.memorywedding.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "guestbook_entry")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuestbookEntry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private WeddingProject project;

    @Column(nullable = false, length = 50)
    private String guestName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(length = 64)
    private String clientKey;

    @Column(nullable = false)
    private int likeCount;

    private LocalDateTime deletedAt;

    @Builder
    public GuestbookEntry(
            WeddingProject project,
            String guestName,
            String message,
            String clientKey) {
        this.project = project;
        this.guestName = guestName;
        this.message = message;
        this.clientKey = clientKey;
        this.likeCount = 0;
    }

    public void updateContent(String guestName, String message) {
        this.guestName = guestName;
        this.message = message;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void increaseLike() {
        this.likeCount += 1;
    }
}
