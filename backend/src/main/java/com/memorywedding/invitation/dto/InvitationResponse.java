package com.memorywedding.invitation.dto;

import com.memorywedding.domain.entity.Invitation;
import com.memorywedding.domain.entity.WeddingProject;
import com.memorywedding.domain.enums.GalleryLayout;
import com.memorywedding.domain.enums.MainPhotoPlacement;
import com.memorywedding.domain.enums.MediaDisplaySize;
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
        String guestPath,
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
    public static InvitationResponse from(
            Invitation invitation,
            List<AccountEntry> accounts,
            String mapUrl,
            InvitationMediaResponse mainPhoto,
            List<InvitationMediaResponse> gallery) {
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
                "/w/" + project.getSlug(),
                invitation.getGalleryLayout() == null
                        ? GalleryLayout.SLIDER
                        : invitation.getGalleryLayout(),
                invitation.getGalleryColumns() <= 0 ? 2 : invitation.getGalleryColumns(),
                invitation.getGalleryImageSize() == null
                        ? MediaDisplaySize.MD
                        : invitation.getGalleryImageSize(),
                invitation.getMainPhotoSize() == null
                        ? MediaDisplaySize.LG
                        : invitation.getMainPhotoSize(),
                invitation.getMainPhotoPlacement() == null
                        ? MainPhotoPlacement.TOP
                        : invitation.getMainPhotoPlacement(),
                invitation.getMainBrightness() <= 0 ? 1.0 : invitation.getMainBrightness(),
                invitation.getMainSaturation() <= 0 ? 1.0 : invitation.getMainSaturation(),
                invitation.getMainFocalX(),
                invitation.getMainFocalY(),
                mainPhoto,
                gallery
        );
    }
}
