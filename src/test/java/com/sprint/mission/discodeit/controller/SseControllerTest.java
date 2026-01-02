package com.sprint.mission.discodeit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.security.DiscodeitUserDetails;
import com.sprint.mission.discodeit.service.SseService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
@DisplayName("SseController 단위 테스트")
public class SseControllerTest {

    @Mock
    private SseService sseService;

    @InjectMocks
    private SseController sseController;

    private DiscodeitUserDetails createUserDetails(UUID userId) {
        User user = new User("username", "user@test.com", "pw", null);
        ReflectionTestUtils.setField(user, "id", userId);
        UserDto dto = new UserDto(userId, "username", "user@test.com", Role.USER, null, true);
        return new DiscodeitUserDetails(dto, "pw", user);
    }

    @Test
    @DisplayName("Last-Event-ID가 있을 때 UUID로 변환하여 연결한다")
    void connect_shouldUseHeaderWhenPresent() {
        // given
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        DiscodeitUserDetails userDetails = createUserDetails(userId);
        SseEmitter emitter = new SseEmitter();
        given(sseService.connect(userId, lastEventId)).willReturn(emitter);

        // when
        SseEmitter result = sseController.connect(userDetails, lastEventId.toString());

        // then
        assertThat(result).isSameAs(emitter);
        then(sseService).should().connect(userId, lastEventId);
    }

    @Test
    @DisplayName("Last-Event-ID가 없으면 null을 전달한다")
    void connect_shouldHandleNullHeader() {
        // given
        UUID userId = UUID.randomUUID();
        DiscodeitUserDetails userDetails = createUserDetails(userId);
        SseEmitter emitter = new SseEmitter();
        given(sseService.connect(userId, null)).willReturn(emitter);

        // when
        SseEmitter result = sseController.connect(userDetails, null);

        // then
        assertThat(result).isSameAs(emitter);
        then(sseService).should().connect(userId, null);
    }
}
