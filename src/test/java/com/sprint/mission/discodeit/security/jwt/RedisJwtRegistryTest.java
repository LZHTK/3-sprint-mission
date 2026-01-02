package com.sprint.mission.discodeit.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.event.message.UserLogInOutEvent;
import com.sprint.mission.discodeit.redis.RedisLockProvider;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RedisJwtRegistry 단위 테스트")
public class RedisJwtRegistryTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisLockProvider redisLockProvider;

    @Mock
    private ListOperations<String, Object> listOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @InjectMocks
    private RedisJwtRegistry redisJwtRegistry;

    private UUID userId;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userDto = new UserDto(userId, "tester", "tester@email.com", Role.USER, null, true);

        ReflectionTestUtils.setField(redisJwtRegistry, "maxActiveJwtCount", 1);
        org.mockito.Mockito.lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
        org.mockito.Mockito.lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    @DisplayName("JWT 등록 시 초과분은 제거하고 인덱스를 갱신한다")
    void registerJwtInformation_shouldTrimAndIndexTokens() {
        // given
        String userKey = "jwt:user:" + userId;
        JwtInformation oldest = new JwtInformation(userDto, "oldAccess", "oldRefresh");
        JwtInformation latest = new JwtInformation(userDto, "newAccess", "newRefresh");
        given(listOperations.size(userKey)).willReturn(2L, 0L);
        given(listOperations.leftPop(userKey)).willReturn(oldest);
        given(redisTemplate.expire(eq(userKey), any(Duration.class))).willReturn(true);

        // when
        redisJwtRegistry.registerJwtInformation(latest);

        // then
        then(setOperations).should().remove("jwt:access_tokens", "oldAccess");
        then(setOperations).should().remove("jwt:refresh_tokens", "oldRefresh");
        then(listOperations).should().rightPush(userKey, latest);
        then(eventPublisher).should().publishEvent(any(UserLogInOutEvent.class));
    }

    @Test
    @DisplayName("사용자 ID로 무효화 시 저장된 토큰과 인덱스를 모두 삭제한다")
    void invalidateJwtInformationByUserId_shouldDeleteTokensAndPublishLogout() {
        // given
        String userKey = "jwt:user:" + userId;
        JwtInformation info = new JwtInformation(userDto, "access", "refresh");
        given(listOperations.range(userKey, 0, -1)).willReturn(List.of(info, "text"));

        // when
        redisJwtRegistry.invalidateJwtInformationByUserId(userId);

        // then
        then(setOperations).should().remove("jwt:access_tokens", "access");
        then(setOperations).should().remove("jwt:refresh_tokens", "refresh");
        then(redisTemplate).should().delete(userKey);
        then(eventPublisher).should().publishEvent(any(UserLogInOutEvent.class));
    }

    @Test
    @DisplayName("리프레시 토큰 회전 시 매칭되는 엔트리를 교체한다")
    void rotateJwtInformation_shouldUpdateMatchingToken() {
        // given
        String userKey = "jwt:user:" + userId;
        JwtInformation stored = new JwtInformation(userDto, "oldA", "targetRefresh");
        JwtInformation rotated = new JwtInformation(userDto, "newA", "newR");
        given(listOperations.range(userKey, 0, -1)).willReturn(List.of(stored));
        given(redisTemplate.expire(eq(userKey), any(Duration.class))).willReturn(true);

        // when
        redisJwtRegistry.rotateJwtInformation("targetRefresh", rotated);

        // then
        then(setOperations).should().remove("jwt:access_tokens", "oldA");
        then(setOperations).should().remove("jwt:refresh_tokens", "targetRefresh");
        then(listOperations).should().set(userKey, 0, stored);
        then(setOperations).should().add("jwt:access_tokens", "newA");
        then(setOperations).should().add("jwt:refresh_tokens", "newR");
    }

    @Test
    @DisplayName("만료 토큰 정리 시 모든 토큰이 만료되면 키를 삭제한다")
    void clearExpiredJwtInformation_shouldRemoveExpiredTokensAndKey() {
        // given
        String userKey = "jwt:user:" + userId;
        JwtInformation expired = new JwtInformation(userDto, "expiredA", "expiredR");
        given(redisTemplate.keys("jwt:user:*")).willReturn(Set.of(userKey));
        given(listOperations.range(userKey, 0, -1)).willReturn(List.of(expired));
        given(jwtTokenProvider.isTokenExpired(anyString())).willReturn(true);

        // when
        redisJwtRegistry.clearExpiredJwtInformation();

        // then
        then(listOperations).should().set(userKey, 0, "EXPIRED");
        then(listOperations).should().remove(userKey, 1, "EXPIRED");
        then(setOperations).should().remove("jwt:access_tokens", "expiredA");
        then(setOperations).should().remove("jwt:refresh_tokens", "expiredR");
        then(redisTemplate).should().delete(userKey);
    }

    @Test
    @DisplayName("활성 토큰 보유 여부는 리스트 크기와 인덱스 멤버십으로 판단한다")
    void hasActiveChecks_shouldRespectRedisState() {
        // given
        String userKey = "jwt:user:" + userId;
        given(listOperations.size(userKey)).willReturn(1L);
        given(setOperations.isMember("jwt:access_tokens", "access")).willReturn(true);
        given(setOperations.isMember("jwt:refresh_tokens", "refresh")).willReturn(false);

        // when
        boolean hasUserTokens = redisJwtRegistry.hasActiveJwtInformationByUserId(userId);
        boolean hasAccess = redisJwtRegistry.hasActiveJwtInformationByAccessToken("access");
        boolean hasRefresh = redisJwtRegistry.hasActiveJwtInformationByRefreshToken("refresh");

        // then
        assertThat(hasUserTokens).isTrue();
        assertThat(hasAccess).isTrue();
        assertThat(hasRefresh).isFalse();
    }
}
