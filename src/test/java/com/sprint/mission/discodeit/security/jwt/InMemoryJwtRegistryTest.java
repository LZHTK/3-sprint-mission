package com.sprint.mission.discodeit.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.Role;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InMemoryJwtRegistryTest {

    @Mock private JwtTokenProvider jwtTokenProvider;

    private InMemoryJwtRegistry jwtRegistry;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        jwtRegistry = new InMemoryJwtRegistry(2, jwtTokenProvider);
        userDto = new UserDto(UUID.randomUUID(), "kim", "kim@sprint.io", Role.USER, (BinaryContentDto) null, true);
    }

    private JwtInformation info(String access, String refresh) {
        return new JwtInformation(userDto, access, refresh);
    }

    @Test
    @DisplayName("최대 세션 수를 초과하면 가장 오래된 세션이 제거된다")
    void registerJwtInformation_최대세션초과() {
        // given: 두 개의 세션이 이미 등록된 상태
        jwtRegistry.registerJwtInformation(info("access-1", "refresh-1"));
        jwtRegistry.registerJwtInformation(info("access-2", "refresh-2"));

        // when: 세 번째 세션을 등록하여 최대치를 초과
        jwtRegistry.registerJwtInformation(info("access-3", "refresh-3"));

        // then: 가장 오래된 refresh-1 세션만 제거됨
        assertThat(jwtRegistry.hasActiveJwtInformationByRefreshToken("refresh-1")).isFalse();
        assertThat(jwtRegistry.hasActiveJwtInformationByRefreshToken("refresh-2")).isTrue();
        assertThat(jwtRegistry.hasActiveJwtInformationByRefreshToken("refresh-3")).isTrue();
    }

    @Test
    @DisplayName("refresh 토큰 교체 시 새 토큰으로 회전된다")
    void rotateJwtInformation_교체() {
        // given: 기존 세션 등록
        jwtRegistry.registerJwtInformation(info("access-old", "refresh-old"));

        // when: refresh-old를 새 토큰으로 교체
        JwtInformation rotated = new JwtInformation(userDto, "access-new", "refresh-new");
        jwtRegistry.rotateJwtInformation("refresh-old", rotated);

        // then: 오래된 토큰은 사라지고 새 토큰이 활성화됨
        assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken("access-old")).isFalse();
        assertThat(jwtRegistry.hasActiveJwtInformationByRefreshToken("refresh-new")).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰을 정리하면 큐에서 제거된다")
    void clearExpiredJwtInformation_만료정리() {
        // given
        jwtRegistry = new InMemoryJwtRegistry(5, jwtTokenProvider);
        jwtRegistry.registerJwtInformation(info("access-active", "refresh-active"));
        jwtRegistry.registerJwtInformation(info("access-expired", "refresh-expired"));

        // access-active, refresh-active, access-expired, refresh-expired 순서로 호출됨
        given(jwtTokenProvider.validateToken("access-active")).willReturn(true);
        given(jwtTokenProvider.validateToken("refresh-active")).willReturn(true);
        given(jwtTokenProvider.validateToken("access-expired")).willReturn(false);
        given(jwtTokenProvider.validateToken("refresh-expired")).willReturn(false);

        // when
        jwtRegistry.clearExpiredJwtInformation();

        // then
        assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken("access-active")).isTrue();
        assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken("access-expired")).isFalse();
    }
}

