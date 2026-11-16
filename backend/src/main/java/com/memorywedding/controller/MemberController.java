package com.memorywedding.controller;

import com.memorywedding.auth.MemberService;
import com.memorywedding.auth.dto.MemberResponse;
import com.memorywedding.auth.dto.UpdateMemberRequest;
import com.memorywedding.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ApiResponse<MemberResponse> me(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(memberService.getMember(memberId));
    }

    @PatchMapping("/me")
    public ApiResponse<MemberResponse> updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateMemberRequest request) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(memberService.updateMember(memberId, request));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        memberService.withdrawMember(memberId);
        return ApiResponse.ok(null, "회원 탈퇴가 완료되었습니다.");
    }
}
