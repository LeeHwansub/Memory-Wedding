package com.memorywedding.guestbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGuestbookRequest(
        @NotBlank @Size(max = 50) String guestName,
        @NotBlank @Size(max = 1000) String message,
        @NotBlank @Size(max = 64) String clientKey
) {
}
