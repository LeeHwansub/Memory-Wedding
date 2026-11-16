package com.memorywedding.upload.dto;

import java.util.List;

public record UploadPageResponse(
        List<UploadFileResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
