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

    @Column(length = 500)
    private String mapUrl;

    @Builder
    public Invitation(WeddingProject project, String title, String greetingMessage) {
        this.project = project;
        this.title = title;
        this.greetingMessage = greetingMessage;
        this.published = false;
    }
}
