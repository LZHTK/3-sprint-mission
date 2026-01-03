package com.sprint.mission.discodeit.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("LoginSuccessHandler 단위 테스트")
public class LoginSuccessHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LoginSuccessHandler handler = new LoginSuccessHandler(objectMapper);

    @Test
    @DisplayName("로그인 성공 시 UserDto를 JSON으로 응답한다")
    void onAuthenticationSuccess_shouldWriteUserDto() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, "tester", "tester@email.com", Role.ADMIN, null, true);
        User user = new User("tester", "tester@email.com", "pw", null);
        ReflectionTestUtils.setField(user, "id", userId);
        DiscodeitUserDetails userDetails = new DiscodeitUserDetails(userDto, "pw", user);
        Authentication authentication = new TestingAuthenticationToken(userDetails, "pw");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        // then
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        assertThat(response.getContentType()).startsWith("application/json");
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("\"username\":\"tester\"");
        assertThat(body).contains(userId.toString());
    }
}
