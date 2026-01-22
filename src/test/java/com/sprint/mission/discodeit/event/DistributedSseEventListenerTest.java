package com.sprint.mission.discodeit.event;

import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.event.message.UserLogInOutEvent;
import com.sprint.mission.discodeit.service.SseService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DistributedSseEventListenerTest {

    @Mock private SseService sseService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DistributedSseEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new DistributedSseEventListener(sseService, objectMapper);
    }

    @Test
    @DisplayName("Kafka Consumer 페이로드를 역직렬화하여 SSE로 브로드캐스트한다")
    void handleUserLogInOutFromKafka_shouldBroadcast() throws Exception {
        // given
        UserLogInOutEvent event = UserLogInOutEvent.logOut(UUID.randomUUID());
        String payload = objectMapper.writeValueAsString(event);

        // when
        listener.handleUserLogInOutFromKafka(payload);

        // then
        then(sseService).should().broadcast("user.status.changed", event);
    }
}
