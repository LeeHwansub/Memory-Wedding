package com.memorywedding.domain.repository;

import com.memorywedding.domain.entity.OAuthAccount;
import com.memorywedding.domain.enums.OAuthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {

    Optional<OAuthAccount> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    java.util.List<OAuthAccount> findByMember_Id(Long memberId);
}
