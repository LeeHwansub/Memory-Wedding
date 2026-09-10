package com.memorywedding.invitation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountEntry(
        @NotBlank @Size(max = 50) String relation,
        @NotBlank @Size(max = 50) String bankName,
        @NotBlank @Size(max = 100) String accountNumber
) {
}
