package com.sprint.mission.discodeit.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.service.distributed.RedisBasedSseService;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.test.util.ReflectionTestUtils;

public class SseRedisConfigTest {

    private MessageListener findListener(
        RedisMessageListenerContainer container, String topicName) {

        @SuppressWarnings("unchecked")
        Map<MessageListener, Set<Topic>> listenerTopics =
            (Map<MessageListener, Set<Topic>>) ReflectionTestUtils.getField(container, "listenerTopics");

        assertThat(listenerTopics).isNotNull();

        return listenerTopics.entrySet().stream()
            .filter(entry -> entry.getValue().stream()
                .anyMatch(topic -> topicName.equals(topic.getTopic())))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElseThrow();
    }

    @Test
    @DisplayName("broadcast 채널 메시지는 handleBroadcastMessage로 전달된다")
    void broadcastMessage_isHandled() throws Exception {
        // given
        ObjectMapper objectMapper = new ObjectMapper();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisBasedSseService sseService = mock(RedisBasedSseService.class);

        SseRedisConfig config = new SseRedisConfig(objectMapper);
        RedisMessageListenerContainer container =
            config.redisMessageListenerContainer(connectionFactory, sseService);

        MessageListener broadcastListener = findListener(container, "sse:broadcast");

        Message broadcastMessage = mock(Message.class);
        when(broadcastMessage.getBody())
            .thenReturn("{\"eventName\":\"ping\",\"data\":\"hello\"}".getBytes());

        // when
        broadcastListener.onMessage(broadcastMessage, null);

        // then
        then(sseService).should().handleBroadcastMessage(any(RedisBasedSseService.SseMessage.class));
    }

    @Test
    @DisplayName("targeted 채널 메시지는 handleTargetedMessage로 전달된다")
    void targetedMessage_isHandled() throws Exception {
        // given
        ObjectMapper objectMapper = new ObjectMapper();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisBasedSseService sseService = mock(RedisBasedSseService.class);

        SseRedisConfig config = new SseRedisConfig(objectMapper);
        RedisMessageListenerContainer container =
            config.redisMessageListenerContainer(connectionFactory, sseService);

        MessageListener targetedListener = findListener(container, "sse:targeted");

        Message targetedMessage = mock(Message.class);
        when(targetedMessage.getBody())
            .thenReturn("{\"eventName\":\"ping\",\"data\":\"hello\",\"targetUsers\":[]}".getBytes());

        // when
        targetedListener.onMessage(targetedMessage, null);

        // then
        then(sseService).should().handleTargetedMessage(any(RedisBasedSseService.SseMessage.class));
    }

    @Test
    @DisplayName("잘못된 JSON이면 예외를 삼키고 처리하지 않는다")
    void invalidJson_isIgnored() {
        // given
        ObjectMapper objectMapper = new ObjectMapper();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisBasedSseService sseService = mock(RedisBasedSseService.class);

        SseRedisConfig config = new SseRedisConfig(objectMapper);
        RedisMessageListenerContainer container =
            config.redisMessageListenerContainer(connectionFactory, sseService);

        MessageListener broadcastListener = findListener(container, "sse:broadcast");

        Message invalidMessage = mock(Message.class);
        when(invalidMessage.getBody()).thenReturn("not-json".getBytes());

        // when
        broadcastListener.onMessage(invalidMessage, null);

        // then
        then(sseService).shouldHaveNoInteractions();
    }
}
