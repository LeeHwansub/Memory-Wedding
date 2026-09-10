package com.memorywedding.auth.dto;

import com.memorywedding.domain.entity.Member;
import com.memorywedding.domain.enums.MemberRole;
import java.time.LocalDateTime;
import java.util.List;

public record MemberResponse(
        Long id,
        String email,
        String displayName,
        MemberRole role,
        List<String> providers,
        LocalDateTime createdAt
) {
    public static MemberResponse from(Member member, List<String> providers) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getDisplayName(),
                member.getRole(),
                providers,
                member.getCreatedAt()
        );
    }
}
