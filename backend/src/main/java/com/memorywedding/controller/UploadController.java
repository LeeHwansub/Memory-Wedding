package com.memorywedding.controller;

import com.memorywedding.common.ApiResponse;
import com.memorywedding.domain.enums.FileType;
import com.memorywedding.upload.UploadService;
import com.memorywedding.upload.dto.UploadFileResponse;
import com.memorywedding.upload.dto.UploadFolderTreeResponse;
import com.memorywedding.upload.dto.UploadPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/api/public/w/{slug}/upload")
    public ApiResponse<UploadFileResponse> uploadPublic(
            @PathVariable String slug,
            @RequestParam("guestName") String guestName,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(uploadService.uploadPublic(slug, guestName, file), "업로드가 완료되었습니다.");
    }

    @GetMapping("/api/public/w/{slug}/uploads")
    public ApiResponse<UploadPageResponse> listPublic(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return ApiResponse.ok(uploadService.listPublic(slug, page, size));
    }

    @GetMapping("/api/projects/{projectId}/uploads/folders")
    public ApiResponse<UploadFolderTreeResponse> folderTreeForOwner(
            Authentication authentication,
            @PathVariable Long projectId) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(uploadService.folderTreeForOwner(memberId, projectId));
    }

    @GetMapping("/api/projects/{projectId}/uploads")
    public ApiResponse<UploadPageResponse> listForOwner(
            Authentication authentication,
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(required = false) FileType fileType,
            @RequestParam(required = false) String guestName) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(
                uploadService.listForOwner(memberId, projectId, page, size, fileType, guestName));
    }

    @GetMapping("/api/projects/{projectId}/uploads/{fileId}/content")
    public ResponseEntity<InputStreamResource> openForOwner(
            Authentication authentication,
            @PathVariable Long projectId,
            @PathVariable Long fileId) {
        Long memberId = (Long) authentication.getPrincipal();
        return uploadService.openForOwner(memberId, projectId, fileId);
    }

    @DeleteMapping("/api/projects/{projectId}/uploads/{fileId}")
    public ApiResponse<Void> deleteForOwner(
            Authentication authentication,
            @PathVariable Long projectId,
            @PathVariable Long fileId) {
        Long memberId = (Long) authentication.getPrincipal();
        uploadService.deleteForOwner(memberId, projectId, fileId);
        return ApiResponse.ok(null, "파일이 삭제되었습니다.");
    }
}
