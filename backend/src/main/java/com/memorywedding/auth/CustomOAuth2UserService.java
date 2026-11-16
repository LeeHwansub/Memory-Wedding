package com.memorywedding.auth;

import com.memorywedding.domain.entity.Member;
import com.memorywedding.domain.entity.OAuthAccount;
import com.memorywedding.domain.repository.MemberRepository;
import com.memorywedding.domain.repository.OAuthAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final OAuthAccountRepository oauthAccountRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, oAuth2User.getAttributes());

        Member member = saveOrUpdate(attributes);

        return new DefaultOAuth2User(
                oAuth2User.getAuthorities(),
                oAuth2User.getAttributes(),
                userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName()
        ) {
            @Override
            public String getName() {
                return String.valueOf(member.getId());
            }
        };
    }

    private Member saveOrUpdate(OAuthAttributes attributes) {
        return oauthAccountRepository
                .findByProviderAndProviderUserId(attributes.provider(), attributes.providerUserId())
                .map(OAuthAccount::getMember)
                .orElseGet(() -> registerNewMember(attributes));
    }

    private Member registerNewMember(OAuthAttributes attributes) {
        Member member = memberRepository.findByEmailAndDeletedAtIsNull(attributes.email())
                .orElseGet(() -> memberRepository.save(attributes.toMember()));

        oauthAccountRepository.save(OAuthAccount.builder()
                .member(member)
                .provider(attributes.provider())
                .providerUserId(attributes.providerUserId())
                .build());

        return member;
    }
}
