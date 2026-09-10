package com.memorywedding.controller;

import com.memorywedding.common.ApiResponse;
import com.memorywedding.guestbook.GuestbookService;
import com.memorywedding.guestbook.dto.CreateGuestbookRequest;
import com.memorywedding.guestbook.dto.GuestbookEntryResponse;
import com.memorywedding.guestbook.dto.GuestbookLikeRequest;
import com.memorywedding.guestbook.dto.GuestbookPageResponse;
import com.memorywedding.guestbook.dto.UpdateGuestbookRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GuestbookController {

    private final GuestbookService guestbookService;

    @GetMapping("/api/public/w/{slug}/guestbook")
    public ApiResponse<GuestbookPageResponse> listPublic(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String clientKey) {
        return ApiResponse.ok(guestbookService.listPublic(slug, page, size, clientKey));
    }

    @PostMapping("/api/public/w/{slug}/guestbook")
    public ApiResponse<GuestbookEntryResponse> createPublic(
            @PathVariable String slug,
            @Valid @RequestBody CreateGuestbookRequest request) {
        return ApiResponse.ok(guestbookService.createPublic(slug, request), "방명록이 등록되었습니다.");
    }

    @PutMapping("/api/public/w/{slug}/guestbook/{entryId}")
    public ApiResponse<GuestbookEntryResponse> updatePublic(
            @PathVariable String slug,
            @PathVariable Long entryId,
            @Valid @RequestBody UpdateGuestbookRequest request) {
        return ApiResponse.ok(guestbookService.updatePublic(slug, entryId, request), "방명록이 수정되었습니다.");
    }

    @PostMapping("/api/public/w/{slug}/guestbook/{entryId}/like")
    public ApiResponse<GuestbookEntryResponse> like(
            @PathVariable String slug,
            @PathVariable Long entryId,
            @Valid @RequestBody GuestbookLikeRequest request) {
        return ApiResponse.ok(guestbookService.likePublic(slug, entryId, request.clientKey()));
    }

    @GetMapping("/api/projects/{projectId}/guestbook")
    public ApiResponse<GuestbookPageResponse> listForOwner(
            Authentication authentication,
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(guestbookService.listForOwner(memberId, projectId, page, size));
    }

    @DeleteMapping("/api/projects/{projectId}/guestbook/{entryId}")
    public ApiResponse<Void> deleteForOwner(
            Authentication authentication,
            @PathVariable Long projectId,
            @PathVariable Long entryId) {
        Long memberId = (Long) authentication.getPrincipal();
        guestbookService.deleteForOwner(memberId, projectId, entryId);
        return ApiResponse.ok(null, "방명록이 삭제되었습니다.");
    }
}
