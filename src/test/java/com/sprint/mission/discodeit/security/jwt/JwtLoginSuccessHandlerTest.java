package com.sprint.mission.discodeit.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.response.JwtDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.security.DiscodeitUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtLoginSuccessHandler 단위 테스트")
public class JwtLoginSuccessHandlerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtRegistry jwtRegistry;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private JwtLoginSuccessHandler handler;

    @Test
    @DisplayName("JWT 로그인 성공 시 access/refresh 발급과 쿠키 설정을 수행한다")
    void onAuthenticationSuccess_shouldIssueTokensAndSetCookie() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, "tester", "tester@email.com", Role.USER, null, true);
        DiscodeitUserDetails userDetails = new DiscodeitUserDetails(userDto, "pw", createUser(userId));
        Authentication authentication = new TestingAuthenticationToken(userDetails, "pw");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(jwtTokenProvider.generateAccessToken(authentication)).willReturn("access-token");
        given(jwtTokenProvider.generateRefreshToken(authentication)).willReturn("refresh-token");
        given(jwtTokenProvider.getRefreshTokenValidityInSeconds()).willReturn(3600L);

        // when
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        // then
        then(jwtRegistry).should().registerJwtInformation(any(JwtInformation.class));
        Cookie refreshCookie = response.getCookie("REFRESH_TOKEN");
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.getValue()).isEqualTo("refresh-token");
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);

        JwtDto dto = objectMapper.readValue(response.getContentAsByteArray(), JwtDto.class);
        assertThat(dto.accessToken()).isEqualTo("access-token");
        assertThat(dto.userDto().username()).isEqualTo("tester");
    }

    @Test
    @DisplayName("토큰 생성 중 예외가 발생하면 500 상태와 오류 메시지를 반환한다")
    void onAuthenticationSuccess_shouldHandleTokenFailure() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        DiscodeitUserDetails userDetails = new DiscodeitUserDetails(
            new UserDto(userId, "tester", "tester@email.com", Role.USER, null, true),
            "pw",
            createUser(userId)
        );
        Authentication authentication = new TestingAuthenticationToken(userDetails, "pw");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(jwtTokenProvider.generateAccessToken(authentication)).willThrow(new IllegalStateException("fail"));

        // when
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        // then
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).contains("error");
    }

    private User createUser(UUID userId) {
        User user = new User("tester", "tester@email.com", "pw", null);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
