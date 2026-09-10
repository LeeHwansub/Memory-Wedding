package com.memorywedding.project.dto;

import com.memorywedding.domain.enums.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record UpdateProjectRequest(
        @NotBlank @Size(max = 50) String groomName,
        @NotBlank @Size(max = 50) String brideName,
        @NotNull LocalDateTime weddingAt,
        @Size(max = 200) String venueName,
        @Size(max = 500) String venueAddress,
        ProjectStatus status
) {
}
