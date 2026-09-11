package com.memorywedding.invitation.dto;

import com.memorywedding.domain.enums.GalleryLayout;
import com.memorywedding.domain.enums.MainPhotoPlacement;
import com.memorywedding.domain.enums.MediaDisplaySize;
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
        String slug,
        GalleryLayout galleryLayout,
        int galleryColumns,
        MediaDisplaySize galleryImageSize,
        MediaDisplaySize mainPhotoSize,
        MainPhotoPlacement mainPhotoPlacement,
        double mainBrightness,
        double mainSaturation,
        int mainFocalX,
        int mainFocalY,
        InvitationMediaResponse mainPhoto,
        List<InvitationMediaResponse> gallery
) {
}
