package com.memorywedding.invitation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateInvitationRequest(
        @Size(max = 200) String title,
        String greetingMessage,
        @Valid List<AccountEntry> accounts
) {
}
