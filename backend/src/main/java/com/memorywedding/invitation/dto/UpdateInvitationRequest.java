package com.memorywedding.invitation.dto;

import com.memorywedding.domain.enums.GalleryLayout;
import com.memorywedding.domain.enums.InvitationTemplate;
import com.memorywedding.domain.enums.MainPhotoPlacement;
import com.memorywedding.domain.enums.MediaDisplaySize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateInvitationRequest(
        @Size(max = 200) String title,
        String greetingMessage,
        @Valid List<AccountEntry> accounts,
        InvitationTemplate template,
        GalleryLayout galleryLayout,
        @Min(2) @Max(4) Integer galleryColumns,
        MediaDisplaySize galleryImageSize,
        MediaDisplaySize mainPhotoSize,
        MainPhotoPlacement mainPhotoPlacement,
        @DecimalMin("0.5") @DecimalMax("1.5") Double mainBrightness,
        @DecimalMin("0.5") @DecimalMax("1.5") Double mainSaturation,
        @Min(0) @Max(100) Integer mainFocalX,
        @Min(0) @Max(100) Integer mainFocalY
) {
}
