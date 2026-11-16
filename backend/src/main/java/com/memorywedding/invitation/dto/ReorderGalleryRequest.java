package com.memorywedding.invitation.dto;

import java.util.List;

public record ReorderGalleryRequest(
        List<Long> galleryIds
) {
}
