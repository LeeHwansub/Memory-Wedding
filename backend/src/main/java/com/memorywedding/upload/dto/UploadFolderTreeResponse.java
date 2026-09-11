package com.memorywedding.upload.dto;

import java.util.List;

public record UploadFolderTreeResponse(
        long photoCount,
        long videoCount,
        List<UploadGuestFolderResponse> photos,
        List<UploadGuestFolderResponse> videos
) {
}
