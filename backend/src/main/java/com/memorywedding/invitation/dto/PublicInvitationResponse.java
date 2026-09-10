package com.memorywedding.invitation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PublicInvitationResponse(
        String title,
        String greetingMessage,
        String mapUrl,
        List<AccountEntry> accounts,
        String groomName,
        String brideName,
        LocalDateTime weddingAt,
        String venueName,
        String venueAddress,
        String slug
) {
}
