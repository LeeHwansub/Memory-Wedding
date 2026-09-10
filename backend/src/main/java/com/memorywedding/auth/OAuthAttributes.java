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
                        (String) response.get("id"),
                        (String) response.get("email"),
                        (String) response.get("name")
                );
            }
        };
    }

    public Member toMember() {
        return Member.builder()
                .email(email)
                .displayName(displayName != null ? displayName : email)
                .role(MemberRole.USER)
                .build();
    }
}
