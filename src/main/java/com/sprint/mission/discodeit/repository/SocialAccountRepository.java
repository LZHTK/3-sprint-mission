package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.SocialAccount;
import com.sprint.mission.discodeit.entity.SocialProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {

    Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
}
