package com.memorywedding.controller;

import com.memorywedding.common.ApiResponse;
import com.memorywedding.invitation.InvitationService;
import com.memorywedding.invitation.dto.InvitationResponse;
import com.memorywedding.invitation.dto.PublicInvitationResponse;
import com.memorywedding.invitation.dto.UpdateInvitationRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/api/public/w/{slug}")
    public ApiResponse<PublicInvitationResponse> getPublic(@PathVariable String slug) {
        return ApiResponse.ok(invitationService.getPublicBySlug(slug));
    }
}
