package com.sprint.mission.discodeit.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.sprint.mission.discodeit.dto.data.MessageDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.MessageService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

public class ResourceSecurityExpressionTest {

    @Test
    @DisplayName("isOwner는 인증되지 않았으면 false를 반환한다")
    void isOwner_returnsFalse_whenUnauthenticated() {
        // given
        MessageService messageService = Mockito.mock(MessageService.class);
        ResourceSecurityExpression expr = new ResourceSecurityExpression(messageService);

        Authentication auth = Mockito.mock(Authentication.class);
        given(auth.isAuthenticated()).willReturn(false);

        // when
        boolean result = expr.isOwner(UUID.randomUUID(), auth);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isOwner는 사용자 ID가 같으면 true를 반환한다")
    void isOwner_returnsTrue_whenSameUser() {
        // given
        MessageService messageService = Mockito.mock(MessageService.class);
        ResourceSecurityExpression expr = new ResourceSecurityExpression(messageService);

        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, "user", "email", Role.USER, null, false);
        User user = new User("user", "email", "pwd", null);
        DiscodeitUserDetails details = new DiscodeitUserDetails(userDto, "pwd", user);

        Authentication auth =
            new UsernamePasswordAuthenticationToken(details, "pwd", details.getAuthorities());

        // when
        boolean result = expr.isOwner(userId, auth);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isMessageAuthor는 인증되지 않았으면 false를 반환한다")
    void isMessageAuthor_returnsFalse_whenUnauthenticated() {
        // given
        MessageService messageService = Mockito.mock(MessageService.class);
        ResourceSecurityExpression expr = new ResourceSecurityExpression(messageService);

        Authentication auth = Mockito.mock(Authentication.class);
        given(auth.isAuthenticated()).willReturn(false);

        // when
        boolean result = expr.isMessageAuthor(UUID.randomUUID(), auth);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isMessageAuthor는 작성자가 현재 사용자면 true를 반환한다")
    void isMessageAuthor_returnsTrue_whenAuthorMatches() {
        // given
        MessageService messageService = Mockito.mock(MessageService.class);
        ResourceSecurityExpression expr = new ResourceSecurityExpression(messageService);

        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, "user", "email", Role.USER, null, false);
        User user = new User("user", "email", "pwd", null);
        DiscodeitUserDetails details = new DiscodeitUserDetails(userDto, "pwd", user);

        Authentication auth =
            new UsernamePasswordAuthenticationToken(details, "pwd", details.getAuthorities());

        MessageDto message = Mockito.mock(MessageDto.class);
        UserDto author = new UserDto(userId, "user", "email", Role.USER, null, false);
        given(message.author()).willReturn(author);
        given(messageService.find(any(UUID.class))).willReturn(message);

        // when
        boolean result = expr.isMessageAuthor(UUID.randomUUID(), auth);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isMessageAuthor는 작성자가 null이면 false를 반환한다")
    void isMessageAuthor_returnsFalse_whenAuthorNull() {
        // given
        MessageService messageService = Mockito.mock(MessageService.class);
        ResourceSecurityExpression expr = new ResourceSecurityExpression(messageService);

        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, "user", "email", Role.USER, null, false);
        User user = new User("user", "email", "pwd", null);
        DiscodeitUserDetails details = new DiscodeitUserDetails(userDto, "pwd", user);

        Authentication auth =
            new UsernamePasswordAuthenticationToken(details, "pwd", details.getAuthorities());

        MessageDto message = Mockito.mock(MessageDto.class);
        given(message.author()).willReturn(null);
        given(messageService.find(any(UUID.class))).willReturn(message);

        // when
        boolean result = expr.isMessageAuthor(UUID.randomUUID(), auth);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isMessageAuthor는 예외 발생 시 false를 반환한다")
    void isMessageAuthor_returnsFalse_whenException() {
        // given
        MessageService messageService = Mockito.mock(MessageService.class);
        ResourceSecurityExpression expr = new ResourceSecurityExpression(messageService);

        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, "user", "email", Role.USER, null, false);
        User user = new User("user", "email", "pwd", null);
        DiscodeitUserDetails details = new DiscodeitUserDetails(userDto, "pwd", user);

        Authentication auth =
            new UsernamePasswordAuthenticationToken(details, "pwd", details.getAuthorities());

        given(messageService.find(any(UUID.class))).willThrow(new RuntimeException("boom"));

        // when
        boolean result = expr.isMessageAuthor(UUID.randomUUID(), auth);

        // then
        assertThat(result).isFalse();
    }
}
