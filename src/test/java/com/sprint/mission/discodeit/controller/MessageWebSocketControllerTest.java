package com.sprint.mission.discodeit.controller;

import static org.mockito.BDDMockito.then;

import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.service.MessageService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageWebSocketController 단위 테스트")
public class MessageWebSocketControllerTest {

    @Mock
    private MessageService messageService;

    @InjectMocks
    private MessageWebSocketController controller;

    @Test
    @DisplayName("메시지 전송 요청은 서비스로 위임된다")
    void sendMessage_shouldDelegateToService() {
        // given
        MessageCreateRequest request = new MessageCreateRequest("hello", UUID.randomUUID(), UUID.randomUUID());

        // when
        controller.sendMessage(request);

        // then
        then(messageService).should().create(request, null);
    }
}
