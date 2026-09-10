package com.memorywedding.guestbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuestbookLikeRequest(
        @NotBlank @Size(max = 64) String clientKey
) {
}
