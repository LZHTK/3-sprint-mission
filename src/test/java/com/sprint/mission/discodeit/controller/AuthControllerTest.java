package com.sprint.mission.discodeit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.response.JwtDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.security.DiscodeitUserDetails;
import com.sprint.mission.discodeit.security.jwt.JwtRegistry;
import com.sprint.mission.discodeit.service.AuthService;
import com.sprint.mission.discodeit.service.TokenRefreshService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;
    @Mock private TokenRefreshService tokenRefreshService;
    @Mock private JwtRegistry jwtRegistry;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    @InjectMocks private AuthController authController;

    @Test
    @DisplayName("리프레시 토큰 쿠키가 존재하면 TokenRefreshService를 호출한다")
    void refreshToken_쿠키사용() {
        // given: REFRESH_TOKEN 쿠키와 JwtDto가 준비되어 있음
        JwtDto jwtDto = new JwtDto(
            new UserDto(UUID.randomUUID(), "kim", "kim@sprint.io", Role.USER, null, true),
            "new-access");
        Cookie[] cookies = {new Cookie("REFRESH_TOKEN", "refresh.jwt")};
        given(request.getCookies()).willReturn(cookies);
        given(tokenRefreshService.refreshTokens("refresh.jwt", request, response)).willReturn(jwtDto);

        // when: 컨트롤러 API 호출
        var result = authController.refreshToken(request, response);

        // then: 서비스 호출 및 ResponseEntity 값 검증
        assertThat(result.getBody()).isEqualTo(jwtDto);
        then(tokenRefreshService).should().refreshTokens("refresh.jwt", request, response);
    }

    @Test
    @DisplayName("쿠키가 없을 경우 TokenRefreshService에 null이 전달된다")
    void refreshToken_쿠키없음() {
        // given
        given(request.getCookies()).willReturn(null);
        given(tokenRefreshService.refreshTokens(null, request, response))
            .willThrow(new IllegalArgumentException("missing"));

        // when
        ThrowingCallable when = () -> authController.refreshToken(request, response);

        // then
        assertThatThrownBy(when).isInstanceOf(IllegalArgumentException.class);
        then(tokenRefreshService).should().refreshTokens(null, request, response);
    }

    @Test
    @DisplayName("유저 역할 변경 API는 JwtRegistry를 통해 강제 로그아웃을 수행한다")
    void updateUserRole_강제로그아웃() {
        // given
        UUID userId = UUID.randomUUID();
        var requestDto = new com.sprint.mission.discodeit.dto.request.UserRoleUpdateRequest(
            userId, Role.ADMIN);
        var adminDetails = org.mockito.Mockito.mock(DiscodeitUserDetails.class);
        var responseDto = new UserDto(userId, "kim", "kim@sprint.io", Role.ADMIN, null, true);
        given(jwtRegistry.hasActiveJwtInformationByUserId(userId)).willReturn(true);
        given(authService.updateUserRole(userId, Role.ADMIN)).willReturn(responseDto);

        // when
        var response = authController.updateUserRole(requestDto, adminDetails);

        // then
        assertThat(response.getBody()).isEqualTo(responseDto);
        then(jwtRegistry).should().invalidateJwtInformationByUserId(userId);
        then(authService).should().updateUserRole(userId, Role.ADMIN);
    }
}

