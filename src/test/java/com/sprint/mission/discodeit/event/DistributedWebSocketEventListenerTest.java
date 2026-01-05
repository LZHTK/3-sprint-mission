package com.sprint.mission.discodeit.event;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.mission.discodeit.dto.data.MessageDto;
import com.sprint.mission.discodeit.service.MessageService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
public class DistributedWebSocketEventListenerTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private MessageService messageService;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private DistributedWebSocketEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new DistributedWebSocketEventListener(
            messagingTemplate, messageService, kafkaTemplate
        );
    }

    @Test
    @DisplayName("트랜잭션 커밋 후 생성 이벤트는 Kafka topic으로 전파된다")
    void handleMessageCreateForKafka_shouldSendEventToTopic() {
        // given
        MessageCreateEvent event = new MessageCreateEvent(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "author", "channel", "hello"
        );

        // when
        listener.handleMessageCreateForKafka(event);

        // then
        then(kafkaTemplate).should().send("message-created", event);
    }

    @Test
    @DisplayName("Kafka Consumer는 메시지 조회 후 WebSocket 구독자에게 전달한다")
    void handleMessageCreateFromKafka_shouldForwardToSubscribers() {
        // given
        UUID messageId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        MessageCreateEvent event = new MessageCreateEvent(
            messageId, channelId, UUID.randomUUID(), "author", "channel", "hello"
        );
        MessageDto dto = new MessageDto(
            messageId, Instant.now(), Instant.now(), "hello", channelId, null, List.of()
        );
        given(messageService.find(messageId)).willReturn(dto);

        // when
        listener.handleMessageCreateFromKafka(event);

        // then
        String expectedDestination = "/sub/channels." + channelId + ".messages";
        then(messageService).should().find(messageId);
        then(messagingTemplate).should().convertAndSend(expectedDestination, dto);
    }
}
