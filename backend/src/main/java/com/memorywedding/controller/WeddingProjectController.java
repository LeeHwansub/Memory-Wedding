package com.memorywedding.controller;

import com.memorywedding.common.ApiResponse;
import com.memorywedding.project.WeddingProjectService;
import com.memorywedding.project.dto.CreateProjectRequest;
import com.memorywedding.project.dto.ProjectResponse;
import com.memorywedding.project.dto.UpdateProjectRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class WeddingProjectController {

    private final WeddingProjectService weddingProjectService;

    @GetMapping
    public ApiResponse<List<ProjectResponse>> list(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(weddingProjectService.listMyProjects(memberId));
    }

    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResponse> get(
            Authentication authentication,
            @PathVariable Long projectId) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(weddingProjectService.getProject(memberId, projectId));
    }

    @PostMapping
    public ApiResponse<ProjectResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateProjectRequest request) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(weddingProjectService.createProject(memberId, request));
    }

    @PutMapping("/{projectId}")
    public ApiResponse<ProjectResponse> update(
            Authentication authentication,
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(weddingProjectService.updateProject(memberId, projectId, request));
    }

    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> delete(
            Authentication authentication,
            @PathVariable Long projectId) {
        Long memberId = (Long) authentication.getPrincipal();
        weddingProjectService.deleteProject(memberId, projectId);
        return ApiResponse.ok(null, "Wedding Project가 삭제되었습니다.");
    }

    @PatchMapping("/{projectId}/invite-link")
    public ApiResponse<ProjectResponse> toggleInvite(
            Authentication authentication,
            @PathVariable Long projectId,
            @RequestBody Map<String, Boolean> body) {
        Long memberId = (Long) authentication.getPrincipal();
        boolean active = Boolean.TRUE.equals(body.get("active"));
        return ApiResponse.ok(weddingProjectService.toggleInviteLink(memberId, projectId, active));
    }
}
