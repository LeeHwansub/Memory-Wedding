package com.memorywedding.guestbook.dto;

import com.memorywedding.domain.entity.GuestbookEntry;
import java.time.LocalDateTime;

public record GuestbookEntryResponse(
        Long id,
        String guestName,
        String message,
        int likeCount,
        boolean likedByMe,
        boolean mine,
        LocalDateTime createdAt
) {
    public static GuestbookEntryResponse from(GuestbookEntry entry, boolean likedByMe, boolean mine) {
        return new GuestbookEntryResponse(
                entry.getId(),
                entry.getGuestName(),
                entry.getMessage(),
                entry.getLikeCount(),
                likedByMe,
                mine,
                entry.getCreatedAt()
        );
    }
}
