package com.memorywedding.controller;

import com.memorywedding.common.ApiResponse;
import com.memorywedding.drive.DriveOAuthService;
import com.memorywedding.drive.DriveSyncService;
import com.memorywedding.drive.dto.DriveConnectUrlResponse;
import com.memorywedding.drive.dto.DriveStatusResponse;
import com.memorywedding.drive.dto.DriveSyncResultResponse;
import com.memorywedding.drive.dto.ProjectDriveStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DriveController {

    private final DriveOAuthService driveOAuthService;
    private final DriveSyncService driveSyncService;

    @GetMapping("/api/drive/status")
    public ApiResponse<DriveStatusResponse> status(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(driveOAuthService.status(memberId));
    }

    @GetMapping("/api/drive/connect-url")
    public ApiResponse<DriveConnectUrlResponse> connectUrl(
            Authentication authentication,
            @RequestParam(required = false) Long projectId) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(driveOAuthService.createAuthorizationUrl(memberId, projectId));
    }

    @GetMapping("/api/drive/oauth/callback")
    public ResponseEntity<Void> callback(
            @RequestParam("code") String code,
            @RequestParam("state") String state) {
        String redirect = driveOAuthService.handleCallback(code, state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirect)
                .build();
    }

    @DeleteMapping("/api/drive/disconnect")
    public ApiResponse<Void> disconnect(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        driveOAuthService.disconnect(memberId);
        return ApiResponse.ok(null, "Drive 연결이 해제되었습니다.");
    }

    @GetMapping("/api/projects/{projectId}/drive")
    public ApiResponse<ProjectDriveStatusResponse> projectStatus(
            Authentication authentication,
            @PathVariable Long projectId) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(driveSyncService.projectStatus(memberId, projectId));
    }

    @PostMapping("/api/projects/{projectId}/drive/folders")
    public ApiResponse<Void> ensureFolders(
            Authentication authentication,
            @PathVariable Long projectId) {
        Long memberId = (Long) authentication.getPrincipal();
        driveSyncService.ensureProjectFolders(memberId, projectId);
        return ApiResponse.ok(null, "Drive 폴더를 준비했습니다.");
    }

    @PostMapping("/api/projects/{projectId}/drive/sync-pending")
    public ApiResponse<DriveSyncResultResponse> syncPending(
            Authentication authentication,
            @PathVariable Long projectId) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(
                driveSyncService.syncPending(memberId, projectId),
                "대기 중인 파일을 Drive로 동기화했습니다.");
    }

    @PostMapping("/api/projects/{projectId}/uploads/{fileId}/sync-drive")
    public ApiResponse<Void> syncFile(
            Authentication authentication,
            @PathVariable Long projectId,
            @PathVariable Long fileId) {
        Long memberId = (Long) authentication.getPrincipal();
        driveSyncService.syncFile(memberId, projectId, fileId);
        return ApiResponse.ok(null, "Drive 사본 업로드가 완료되었습니다.");
    }
}
