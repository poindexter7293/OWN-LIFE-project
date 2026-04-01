package com.ownlife.repository;

import com.ownlife.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUserId(SocialAccount.Provider provider, String providerUserId);

    Optional<SocialAccount> findByMemberMemberIdAndProvider(Long memberId, SocialAccount.Provider provider);
}

