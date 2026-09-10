package com.memorywedding.project.dto;

import com.memorywedding.domain.entity.InviteLink;
import com.memorywedding.domain.entity.WeddingProject;
import com.memorywedding.domain.enums.ProjectStatus;
import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String slug,
        String groomName,
        String brideName,
        LocalDateTime weddingAt,
        String venueName,
        String venueAddress,
        ProjectStatus status,
        String inviteToken,
        boolean inviteActive,
        String guestPath,
        LocalDateTime createdAt
) {
    public static ProjectResponse from(WeddingProject project, InviteLink inviteLink) {
        return new ProjectResponse(
                project.getId(),
                project.getSlug(),
                project.getGroomName(),
                project.getBrideName(),
                project.getWeddingAt(),
                project.getVenueName(),
                project.getVenueAddress(),
                project.getStatus(),
                inviteLink != null ? inviteLink.getToken() : null,
                inviteLink != null && inviteLink.isActive(),
                "/w/" + project.getSlug(),
                project.getCreatedAt()
        );
    }
}
