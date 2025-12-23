package com.sprint.mission.discodeit.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.security.DiscodeitUserDetails;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        // given: 테스트용 secret과 Authentication 구성
        tokenProvider = new JwtTokenProvider("test-secret-12345678901234567890", 5, 1);
        var userDto = new UserDto(UUID.randomUUID(), "kim", "kim@sprint.io", Role.USER, null, true);
        var userDetails = new DiscodeitUserDetails(userDto, "encoded-pw", null);
        authentication = new UsernamePasswordAuthenticationToken(
            userDetails,
            "encoded-pw",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("Access 토큰을 생성하고 즉시 검증할 수 있다")
    void generateAndValidateAccessToken() {
        // when
        String accessToken = tokenProvider.generateAccessToken(authentication);

        // then
        assertThat(tokenProvider.validateToken(accessToken)).isTrue();
        assertThat(tokenProvider.extractUsername(accessToken)).isEqualTo("kim");
        assertThat(tokenProvider.extractRoles(accessToken)).contains("ROLE_USER");
        assertThat(tokenProvider.getTokenType(accessToken)).isEqualTo("access");
    }

    @Test
    @DisplayName("Refresh 토큰을 생성하면 타입이 refresh로 설정된다")
    void generateRefreshToken() {
        // when
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        // then
        assertThat(tokenProvider.validateToken(refreshToken)).isTrue();
        assertThat(tokenProvider.getTokenType(refreshToken)).isEqualTo("refresh");
    }

    @Test
    @DisplayName("토큰 만료 시간 계산 및 만료 여부 확인이 가능하다")
    void tokenExpirationHelpers() {
        String accessToken = tokenProvider.generateAccessToken(authentication);

        // when
        long validitySeconds = tokenProvider.getTokenExpirationTime(accessToken);
        Date expirationDate = tokenProvider.getExpirationDate(accessToken);

        // then
        assertThat(validitySeconds).isPositive();
        assertThat(expirationDate).isNotNull();
        assertThat(tokenProvider.isTokenExpired(accessToken)).isFalse();
    }
}
