package com.memorywedding.guestbook.dto;

import java.util.List;

public record GuestbookPageResponse(
        List<GuestbookEntryResponse> content,
        GuestbookEntryResponse myEntry,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
