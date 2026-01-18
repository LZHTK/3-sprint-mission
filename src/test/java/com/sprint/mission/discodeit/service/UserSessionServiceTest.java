package com.sprint.mission.discodeit.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.DURATION;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class UserSessionServiceTest {

    @Test
    @DisplayName("markUserOnline 이후 온라인 상태가 된다")
    void markUserOnline_shouldBeOnline() {
        // given
        UserSessionService service = new UserSessionService();
        UUID userId = UUID.randomUUID();

        // when
        service.markUserOnline(userId);

        // then
        assertThat(service.isUserOnline(userId)).isTrue();
    }

    @Test
    @DisplayName("markUserOffline 이후 오프라인 상태가 된다")
    void markUserOffline_shouldBeOffline() {
        // given
        UserSessionService service = new UserSessionService();
        UUID userId = UUID.randomUUID();

        service. markUserOnline(userId);

        // when
        service.markUserOffline(userId);

        // then
        assertThat(service.isUserOnline(userId)).isFalse();
    }

    @Test
    @DisplayName("getOnlineUsers가 만료된 세션을 제외한다")
    void getOnlineUsers_shouldFilterExpired() {
        // given
        UserSessionService service = new UserSessionService();
        UUID activeId = UUID.randomUUID();
        UUID expiredId = UUID.randomUUID();

        @SuppressWarnings("unchecked")
        Map<UUID, Instant> sessions =
            (Map<UUID, Instant>) ReflectionTestUtils.getField(service, "activeSessions");

        sessions.put(activeId, Instant.now());
        sessions.put(expiredId, Instant.now().minus(Duration.ofMinutes(31)));

        // when
        Set<UUID> online = service.getOnlineUsers();

        // then
        assertThat(online).contains(activeId);
        assertThat(online).doesNotContain(expiredId);
    }

    @Test
    @DisplayName("cleanExpiredSessions가 만료된 세션을 제거한다")
    void cleanExpiredSessions_shouldRemoveExpired() {
        // given
        UserSessionService service = new UserSessionService();
        UUID expiredId = UUID.randomUUID();

        @SuppressWarnings("unchecked")
        Map<UUID, Instant> sessions =
            (Map<UUID, Instant>) ReflectionTestUtils.getField(service, "activeSessions");

        sessions.put(expiredId, Instant.now().minus(Duration.ofMinutes(31)));

        // when
        service.cleanExpiredSessions();

        // then
        assertThat(service.isUserOnline(expiredId)).isFalse();
    }
}
