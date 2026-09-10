package com.memorywedding.auth;

import com.memorywedding.domain.entity.Member;
import com.memorywedding.domain.enums.MemberRole;
import com.memorywedding.domain.enums.OAuthProvider;
import java.util.Map;

public record OAuthAttributes(
        OAuthProvider provider,
        String providerUserId,
        String email,
        String displayName
) {
    public static OAuthAttributes of(String registrationId, Map<String, Object> attributes) {
        OAuthProvider provider = OAuthProvider.fromRegistrationId(registrationId);

        return switch (provider) {
            case GOOGLE -> new OAuthAttributes(
                    provider,
                    (String) attributes.get("sub"),
                    (String) attributes.get("email"),
                    (String) attributes.get("name")
            );
            case NAVER -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                yield new OAuthAttributes(
                        provider,
                        String.valueOf(response.get("id")),
                        (String) response.get("email"),
                        (String) response.get("name")
                );
            }
            case KAKAO -> {
                String providerUserId = String.valueOf(attributes.get("id"));
                @SuppressWarnings("unchecked")
                Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
                String email = null;
                String nickname = null;
                if (account != null) {
                    email = (String) account.get("email");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> profile = (Map<String, Object>) account.get("profile");
                    if (profile != null) {
                        nickname = (String) profile.get("nickname");
                    }
                }
                if (email == null || email.isBlank()) {
                    email = "kakao_" + providerUserId + "@kakao.local";
                }
                yield new OAuthAttributes(provider, providerUserId, email, nickname);
            }
        };
    }

    public Member toMember() {
        return Member.builder()
                .email(email)
                .displayName(displayName != null && !displayName.isBlank() ? displayName : email)
                .role(MemberRole.USER)
                .build();
    }
}
