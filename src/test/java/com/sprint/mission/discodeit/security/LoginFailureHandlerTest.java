package com.sprint.mission.discodeit.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

@DisplayName("LoginFailureHandler 단위 테스트")
public class LoginFailureHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LoginFailureHandler handler = new LoginFailureHandler(objectMapper);

    @Test
    @DisplayName("로그인 실패 시 401 코드와 오류 메시지를 반환한다")
    void onAuthenticationFailure_shouldReturnErrorJson() throws Exception {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();
        BadCredentialsException exception = new BadCredentialsException("invalid");

        // when
        handler.onAuthenticationFailure(new MockHttpServletRequest(), response, exception);

        // then
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentType()).startsWith("application/json");
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("\"error\"");
        assertThat(body).contains("Authentication Failed");
    }
}
