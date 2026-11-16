package com.memorywedding.drive.dto;

public record DriveStatusResponse(
        boolean connected,
        String googleAccountEmail,
        boolean hasAppRootFolder
) {
}
