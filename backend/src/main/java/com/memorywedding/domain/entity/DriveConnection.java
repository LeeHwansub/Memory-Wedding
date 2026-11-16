package com.memorywedding.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "drive_connection")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DriveConnection extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    /** AES-encrypted refresh token */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String refreshToken;

    @Column(length = 100)
    private String driveRootFolderId;

    @Column(length = 255)
    private String googleAccountEmail;

    private LocalDateTime tokenExpiresAt;

    @Builder
    public DriveConnection(Member member, String refreshToken, String googleAccountEmail) {
        this.member = member;
        this.refreshToken = refreshToken;
        this.googleAccountEmail = googleAccountEmail;
    }

    public void updateTokens(String refreshToken, String googleAccountEmail, LocalDateTime tokenExpiresAt) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            this.refreshToken = refreshToken;
        }
        if (googleAccountEmail != null && !googleAccountEmail.isBlank()) {
            this.googleAccountEmail = googleAccountEmail;
        }
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public void updateDriveRootFolderId(String folderId) {
        this.driveRootFolderId = folderId;
    }
}
