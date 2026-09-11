package com.memorywedding.controller;

import com.memorywedding.common.ApiResponse;
import com.memorywedding.domain.enums.InvitationMediaType;
import com.memorywedding.invitation.InvitationService;
import com.memorywedding.invitation.dto.InvitationMediaResponse;
import com.memorywedding.invitation.dto.InvitationResponse;
import com.memorywedding.invitation.dto.PublicInvitationResponse;
import com.memorywedding.invitation.dto.ReorderGalleryRequest;
import com.memorywedding.invitation.dto.UpdateInvitationRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @GetMapping("/api/projects/{projectId}/invitation")
    public ApiResponse<InvitationResponse> get(
            Authentication authentication,
            @PathVariable Long projectId) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(invitationService.getForOwner(memberId, projectId));
    }

    @PutMapping("/api/projects/{projectId}/invitation")
    public ApiResponse<InvitationResponse> update(
            Authentication authentication,
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateInvitationRequest request) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(invitationService.updateForOwner(memberId, projectId, request));
    }

    @PatchMapping("/api/projects/{projectId}/invitation/publish")
    public ApiResponse<InvitationResponse> publish(
            Authentication authentication,
            @PathVariable Long projectId,
            @RequestBody Map<String, Boolean> body) {
        Long memberId = (Long) authentication.getPrincipal();
        boolean published = Boolean.TRUE.equals(body.get("published"));
        return ApiResponse.ok(invitationService.setPublished(memberId, projectId, published));
    }

    @PostMapping("/api/projects/{projectId}/invitation/media")
    public ApiResponse<InvitationMediaResponse> uploadMedia(
            Authentication authentication,
            @PathVariable Long projectId,
            @RequestParam("type") InvitationMediaType type,
            @RequestParam("file") MultipartFile file) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(
                invitationService.uploadMedia(memberId, projectId, type, file),
                "사진이 업로드되었습니다.");
    }

    @DeleteMapping("/api/projects/{projectId}/invitation/media/{mediaId}")
    public ApiResponse<Void> deleteMedia(
            Authentication authentication,
            @PathVariable Long projectId,
            @PathVariable Long mediaId) {
        Long memberId = (Long) authentication.getPrincipal();
        invitationService.deleteMedia(memberId, projectId, mediaId);
        return ApiResponse.ok(null, "사진이 삭제되었습니다.");
    }

    @PutMapping("/api/projects/{projectId}/invitation/gallery/order")
    public ApiResponse<InvitationResponse> reorderGallery(
            Authentication authentication,
            @PathVariable Long projectId,
            @RequestBody ReorderGalleryRequest request) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(invitationService.reorderGallery(memberId, projectId, request));
    }

    @GetMapping("/api/projects/{projectId}/invitation/media/{mediaId}/content")
    public ResponseEntity<InputStreamResource> openOwnerMedia(
            Authentication authentication,
            @PathVariable Long projectId,
            @PathVariable Long mediaId) {
        Long memberId = (Long) authentication.getPrincipal();
        return invitationService.openMediaForOwner(memberId, projectId, mediaId);
    }

    @GetMapping("/api/public/w/{slug}")
    public ApiResponse<PublicInvitationResponse> getPublic(@PathVariable String slug) {
        return ApiResponse.ok(invitationService.getPublicBySlug(slug));
    }

    @GetMapping("/api/public/w/{slug}/media/{mediaId}/content")
    public ResponseEntity<InputStreamResource> openPublicMedia(
            @PathVariable String slug,
            @PathVariable Long mediaId) {
        return invitationService.openMediaPublic(slug, mediaId);
    }
}
