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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "guestbook_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_guestbook_like_entry_client",
                columnNames = {"entry_id", "client_key"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuestbookLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false)
    private GuestbookEntry entry;

    @Column(name = "client_key", nullable = false, length = 64)
    private String clientKey;

    @Builder
    public GuestbookLike(GuestbookEntry entry, String clientKey) {
        this.entry = entry;
        this.clientKey = clientKey;
    }
}
