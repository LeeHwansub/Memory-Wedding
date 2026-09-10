package com.memorywedding.invitation.dto;

import com.memorywedding.domain.entity.Invitation;
import com.memorywedding.domain.entity.WeddingProject;
import java.time.LocalDateTime;
import java.util.List;

public record InvitationResponse(
        Long id,
        Long projectId,
        String title,
        String greetingMessage,
        boolean published,
        String venueName,
        String venueAddress,
        String mapUrl,
        List<AccountEntry> accounts,
        String groomName,
        String brideName,
        LocalDateTime weddingAt,
        String slug,
        String guestPath
) {
    public static InvitationResponse from(
            Invitation invitation,
            List<AccountEntry> accounts,
            String mapUrl) {
        WeddingProject project = invitation.getProject();
        return new InvitationResponse(
                invitation.getId(),
                project.getId(),
                invitation.getTitle(),
                invitation.getGreetingMessage(),
                invitation.isPublished(),
                project.getVenueName(),
                project.getVenueAddress(),
                mapUrl,
                accounts,
                project.getGroomName(),
                project.getBrideName(),
                project.getWeddingAt(),
                project.getSlug(),
                "/w/" + project.getSlug()
        );
    }
}
