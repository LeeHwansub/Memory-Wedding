package com.memorywedding.auth;

import com.memorywedding.auth.dto.MemberResponse;
import com.memorywedding.auth.dto.UpdateMemberRequest;
import com.memorywedding.domain.entity.Member;
import com.memorywedding.domain.repository.MemberRepository;
import com.memorywedding.domain.repository.OAuthAccountRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final OAuthAccountRepository oauthAccountRepository;

    public MemberResponse getMember(Long memberId) {
        Member member = getActiveMember(memberId);
        List<String> providers = oauthAccountRepository.findByMember_Id(memberId).stream()
                .map(account -> account.getProvider().name())
                .toList();
        return MemberResponse.from(member, providers);
    }

    @Transactional
    public MemberResponse updateMember(Long memberId, UpdateMemberRequest request) {
        Member member = getActiveMember(memberId);
        member.updateDisplayName(request.displayName());
        return getMember(memberId);
    }

    @Transactional
    public void withdrawMember(Long memberId) {
        Member member = getActiveMember(memberId);
        member.withdraw();
    }

    private Member getActiveMember(Long memberId) {
        return memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
    }
}
