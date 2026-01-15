package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.SocialAccount;
import com.sprint.mission.discodeit.entity.SocialProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {

    @Query("""
    select sa from SocialAccount sa
    join fetch sa.user u
    left join fetch u.profile
    where sa.provider = :provider
      and sa.providerUserId = :providerUserId
    """)
    Optional<SocialAccount> findByProviderAndProviderUserIdWithUser(
        SocialProvider provider, String providerUserId
    );
}
