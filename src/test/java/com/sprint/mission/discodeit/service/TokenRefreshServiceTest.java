package com.sprint.mission.discodeit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.response.JwtDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.exception.auth.InvalidRefreshTokenException;
import com.sprint.mission.discodeit.exception.auth.RefreshTokenNotFoundException;
import com.sprint.mission.discodeit.security.DiscodeitUserDetails;
import com.sprint.mission.discodeit.security.DiscodeitUserDetailsService;
import com.sprint.mission.discodeit.security.jwt.JwtInformation;
import com.sprint.mission.discodeit.security.jwt.JwtRegistry;
import com.sprint.mission.discodeit.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class TokenRefreshServiceTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private JwtRegistry jwtRegistry;
    @Mock private DiscodeitUserDetailsService userDetailsService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @InjectMocks private TokenRefreshService tokenRefreshService;

    @Test
    @DisplayName("리프레시 토큰이 유효하면 토큰을 회전시키고 쿠키를 설정한다")
    void refreshTokens_리프레시토큰정상회전() {
        // given: 유효한 리프레시 토큰과 사용자 정보가 준비되어 있음
        String refreshToken = "refresh.jwt";
        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, "kim", "kim@sprint.io", Role.USER, null, true);
        DiscodeitUserDetails userDetails = mock(DiscodeitUserDetails.class);
        Collection<? extends GrantedAuthority> authorities =
            List.of(new SimpleGrantedAuthority("ROLE_USER"));
        given(jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getTokenType(refreshToken)).willReturn("refresh");
        given(jwtTokenProvider.extractUsername(refreshToken)).willReturn("kim");
        given(userDetailsService.loadUserByUsername("kim")).willReturn(userDetails);
        given(userDetails.getUserDto()).willReturn(userDto);
        BDDMockito.<Collection<? extends GrantedAuthority>>given(userDetails.getAuthorities())
            .willReturn(authorities);
        given(jwtTokenProvider.generateAccessToken(any())).willReturn("new-access");
        given(jwtTokenProvider.generateRefreshToken(any())).willReturn("new-refresh");
        given(jwtTokenProvider.getRefreshTokenValidityInSeconds()).willReturn(3600L);
        given(request.isSecure()).willReturn(true);

        // when: refreshTokens 실행
        JwtDto result = tokenRefreshService.refreshTokens(refreshToken, request, response);

        // then: JwtRegistry 회전과 쿠키 설정, DTO 반환이 모두 검증됨
        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.userDto()).isEqualTo(userDto);
        then(jwtRegistry).should().rotateJwtInformation(eq(refreshToken), any(JwtInformation.class));
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        then(response).should().addCookie(cookieCaptor.capture());
        Cookie cookie = cookieCaptor.getValue();
        assertThat(cookie.getName()).isEqualTo("REFRESH_TOKEN");
        assertThat(cookie.getValue()).isEqualTo("new-refresh");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
    }

    @Test
    @DisplayName("리프레시 토큰이 없으면 예외가 발생한다")
    void refreshTokens_토큰없으면예외() {
        // given
        // when
        ThrowingCallable when = () -> tokenRefreshService.refreshTokens(null, request, response);

        // then
        assertThatThrownBy(when).isInstanceOf(RefreshTokenNotFoundException.class);
    }

    @Test
    @DisplayName("토큰 타입이 refresh가 아니면 예외가 발생한다")
    void refreshTokens_토큰타입불일치() {
        // given: 레지스트리에 존재하지만 타입이 access인 리프레시 토큰
        String refreshToken = "invalid-refresh";
        given(jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getTokenType(refreshToken)).willReturn("access");

        // when
        ThrowingCallable when = () ->
            tokenRefreshService.refreshTokens(refreshToken, request, response);

        // then
        assertThatThrownBy(when).isInstanceOf(InvalidRefreshTokenException.class);
        then(jwtRegistry).should().hasActiveJwtInformationByRefreshToken(refreshToken);
        then(jwtRegistry).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("리프레시 토큰이 없으면 RefreshTokenNotFoundException이 발생한다")
    void refreshTokens_missingToken() {
        // given: null 토큰
        // when
        ThrowingCallable when = () -> tokenRefreshService.refreshTokens(null, request, response);

        // then
        assertThatThrownBy(when).isInstanceOf(RefreshTokenNotFoundException.class);
    }

    @Test
    @DisplayName("토큰 타입이 refresh가 아니면 InvalidRefreshTokenException이 발생한다")
    void refreshTokens_invalidType() {
        // given: 레지스트리와 검증은 통과하지만 타입이 access 인 토큰
        String token = "access-token";
        given(jwtRegistry.hasActiveJwtInformationByRefreshToken(token)).willReturn(true);
        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.getTokenType(token)).willReturn("access");

        // when
        ThrowingCallable when = () -> tokenRefreshService.refreshTokens(token, request, response);

        // then
        assertThatThrownBy(when).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("JwtRegistry에 등록되지 않은 리프레시 토큰이면 InvalidRefreshTokenException이 발생한다")
    void refreshTokens_inactiveRefreshToken() {
        // given
        String refreshToken = "stale";
        given(jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)).willReturn(false);

        // when
        ThrowingCallable when = () -> tokenRefreshService.refreshTokens(refreshToken, request, response);

        // then
        assertThatThrownBy(when).isInstanceOf(InvalidRefreshTokenException.class);
        then(jwtTokenProvider).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("validateToken이 실패하면 InvalidRefreshTokenException을 던진다")
    void refreshTokens_invalidSignature() {
        // given
        String refreshToken = "invalid-signature";
        given(jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(false);

        // when
        ThrowingCallable when = () -> tokenRefreshService.refreshTokens(refreshToken, request, response);

        // then
        assertThatThrownBy(when).isInstanceOf(InvalidRefreshTokenException.class);
        then(jwtTokenProvider).should().validateToken(refreshToken);
        then(jwtTokenProvider).shouldHaveNoMoreInteractions();
    }

}

