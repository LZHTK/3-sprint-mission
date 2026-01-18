package com.sprint.mission.discodeit.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.discodeit.config.OAuthProperties;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.security.DiscodeitUserDetailsService;
import com.sprint.mission.discodeit.security.LoginFailureHandler;
import com.sprint.mission.discodeit.security.jwt.JwtAuthenticationFilter;
import com.sprint.mission.discodeit.security.jwt.JwtLoginSuccessHandler;
import com.sprint.mission.discodeit.security.jwt.JwtLogoutHandler;
import com.sprint.mission.discodeit.security.jwt.JwtRegistry;
import com.sprint.mission.discodeit.security.jwt.JwtTokenProvider;
import com.sprint.mission.discodeit.service.SocialOAuthService;
import com.sprint.mission.discodeit.service.SocialOAuthService.SocialLoginResult;
import com.sprint.mission.discodeit.service.UserSessionService;
import jakarta.servlet.http.Cookie;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(SocialAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SocialAuthController 단위 테스트")
public class SocialAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private SocialOAuthService socialOAuthService;

    @MockitoBean private OAuthProperties oAuthProperties;

    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean private JwtLoginSuccessHandler jwtLoginSuccessHandler;
    @MockitoBean private JwtLogoutHandler jwtLogoutHandler;

    @MockitoBean private LoginFailureHandler loginFailureHandler;

    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private JwtRegistry jwtRegistry;

    @MockitoBean private DiscodeitUserDetailsService discodeitUserDetailsService;

    @MockitoBean private UserSessionService userSessionService;

    @Test
    @DisplayName("리다이렉트 요청 시 상태 쿠키를 만들고 인가 URL로 리다이렉트한다")
    void redirect_shouldSetStateCookieAndRedirect() throws Exception {
        // given
        URI authUri = URI.create("https://oauth.example.com/authorize?x=1");
        Mockito.when(socialOAuthService.buildAuthorizationUri(eq("google"), anyString()))
            .thenReturn(authUri);

        // when
        ResultActions result = mockMvc.perform(
            get("/auth/social/redirect").param("provider", "google")
        );

        // then
        result.andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(authUri.toString()))
            .andExpect(cookie().exists("OAUTH_STATE"))
            .andExpect(cookie().httpOnly("OAUTH_STATE", true))
            .andExpect(cookie().path("OAUTH_STATE", "/"));
    }

    @Test
    @DisplayName("state가 유효하지 않으면 invalid_state로 에러 리다이렉트한다")
    void callback_shouldRedirectWhenStateInvalid() throws Exception {
        // given
        Mockito.when(oAuthProperties.getFrontendErrorRedirect())
            .thenReturn("https://front.example.com/error");

        // when
        ResultActions result = mockMvc.perform(get("/auth/social/callback/google")
            .param("state", "bad")
            .cookie(new Cookie("OAUTH_STATE", "good")));

        // then
        result.andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("https://front.example.com/error?error=invalid_state"));
    }

    @Test
    @DisplayName("code가 없으면 missing_code로 에러 리다이렉트한다")
    void callback_shouldRedirectWhenCodeMissing() throws Exception {
        // given
        Mockito.when(oAuthProperties.getFrontendErrorRedirect())
            .thenReturn("https://front.example.com/error");

        // when
        ResultActions result = mockMvc.perform(get("/auth/social/callback/google")
            .param("state", "state-1")
            .cookie(new Cookie("OAUTH_STATE", "state-1")));

        // then
        result.andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("https://front.example.com/error?error=missing_code"));
    }

    @Test
    @DisplayName("정상 콜백이면 리프레시 쿠키를 설정하고 성공 URL로 리다이렉트한다")
    void callback_shouldSetRefreshCookieAndRedirectSuccess() throws Exception {
        // given
        Mockito.when(oAuthProperties.getFrontendSuccessRedirect())
            .thenReturn("https://front.example.com/success");

        UserDto userDto = new UserDto(
            UUID.randomUUID(), "kim", "kim@test.com", Role.USER, null, false
        );
        String accessToken = "access token";
        String refreshToken = "refresh-token";

        Mockito.when(socialOAuthService.handleCallback("google", "code-1"))
            .thenReturn(new SocialLoginResult(userDto, accessToken, refreshToken));

        String expectedToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        String expectedRedirect = "https://front.example.com/success?token=" + expectedToken;

        // when
        ResultActions result = mockMvc.perform(get("/auth/social/callback/google")
            .param("code", "code-1")
            .param("state", "state-1")
            .cookie(new Cookie("OAUTH_STATE", "state-1")));

        // then
        result.andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(expectedRedirect))
            .andExpect(cookie().exists("REFRESH_TOKEN"))
            .andExpect(cookie().httpOnly("REFRESH_TOKEN", true))
            .andExpect(cookie().secure("REFRESH_TOKEN", false))
            .andExpect(cookie().path("REFRESH_TOKEN", "/"));
    }

    @Test
    @DisplayName("콜백 처리 중 예외가 나면 oauth_failed로 에러 리다이렉트한다")
    void callback_shouldRedirectWhenExceptionOccurs() throws Exception {
        // given
        Mockito.when(oAuthProperties.getFrontendErrorRedirect())
            .thenReturn("https://front.example.com/error");
        Mockito.when(socialOAuthService.handleCallback("google", "code-1"))
            .thenThrow(new RuntimeException("boom"));

        // when
        ResultActions result = mockMvc.perform(get("/auth/social/callback/google")
            .param("code", "code-1")
            .param("state", "state-1")
            .cookie(new Cookie("OAUTH_STATE", "state-1")));

        // then
        result.andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("https://front.example.com/error?error=oauth_failed"));
    }
}
