package com.sprint.mission.discodeit.service.distributed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class RedisBasedSseServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private RedisBasedSseService redisBasedSseService;

    @BeforeEach
    void setUp() {
        redisBasedSseService = new RedisBasedSseService(redisTemplate, new ObjectMapper());
    }


    @Test
    @DisplayName("타깃 전송 요청은 Redis targeted 채널로 publish 된다")
    void send_타깃메시지발행() {
        // given: 수신자 목록
        List<UUID> receivers = List.of(UUID.randomUUID(), UUID.randomUUID());

        // when: send 호출
        redisBasedSseService.send(receivers, "notifications.new", "payload");

        // then: Redis targeted 채널로 메시지가 publish 됨
        then(redisTemplate).should()
            .convertAndSend(eq("sse:targeted"), any(RedisBasedSseService.SseMessage.class));
    }

    @Test
    @DisplayName("send 호출 시 Redis targeted 채널로 메시지를 publish 한다")
    void send_publishesTargetedChannel() {
        // given
        RedisBasedSseService service = new RedisBasedSseService(redisTemplate, new ObjectMapper());

        // when
        service.send(List.of(UUID.randomUUID()), "notifications.new", "payload");

        // then
        then(redisTemplate).should()
            .convertAndSend(eq("sse:targeted"), any(RedisBasedSseService.SseMessage.class));
    }

    @Test
    @DisplayName("브로드캐스트 요청은 Redis broadcast 채널로 publish 된다")
    void broadcast_브로드캐스트발행() {
        // given / when
        redisBasedSseService.broadcast("system.announcement", "payload");

        // then
        then(redisTemplate).should()
            .convertAndSend(eq("sse:broadcast"), any(RedisBasedSseService.SseMessage.class));
    }

    @Test
    @DisplayName("로컬 커넥션이 존재하면 handleTargetedMessage에서 각 Emitter로 이벤트가 전달된다")
    void handleTargetedMessage_로컬전달및오류제거() throws Exception {
        // given: 성공 emitter와 실패 emitter를 로컬 연결 맵에 등록
        @SuppressWarnings("unchecked")
        ConcurrentMap<UUID, SseEmitter> connections =
            (ConcurrentMap<UUID, SseEmitter>) ReflectionTestUtils.getField(
                redisBasedSseService, "localConnections");

        UUID okUser = UUID.randomUUID();
        UUID failUser = UUID.randomUUID();
        RecordingEmitter okEmitter = new RecordingEmitter();
        FailingEmitter failingEmitter = new FailingEmitter();
        connections.put(okUser, okEmitter);
        connections.put(failUser, failingEmitter);

        RedisBasedSseService.SseMessage message =
            new RedisBasedSseService.SseMessage(List.of(okUser, failUser), "notifications.new", "body");

        // when: targeted 메시지 처리
        redisBasedSseService.handleTargetedMessage(message);

        // then: 정상 emitter는 호출되고, 실패 emitter는 맵에서 제거됨
        assertThat(okEmitter.invoked).isTrue();
        assertThat(connections).containsKey(okUser);
        assertThat(connections).doesNotContainKey(failUser);
    }

    @Test
    @DisplayName("handleBroadcastMessage는 등록된 emitter 모두에 데이터를 전달한다")
    void handleBroadcastMessage_deliversToEmitters() throws Exception {
        // given
        RedisBasedSseService service = new RedisBasedSseService(redisTemplate, new ObjectMapper());
        UUID userId = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);
        @SuppressWarnings("unchecked")
        ConcurrentMap<UUID, SseEmitter> connections =
            (ConcurrentMap<UUID, SseEmitter>) ReflectionTestUtils.getField(service, "localConnections");
        connections.put(userId, emitter);

        // when
        service.handleBroadcastMessage(new RedisBasedSseService.SseMessage(null, "ping", "data"));

        // then
        then(emitter).should().send(any(SseEmitter.SseEventBuilder.class));
    }

    private static class RecordingEmitter extends SseEmitter {
        boolean invoked = false;

        RecordingEmitter() {
            super(Long.MAX_VALUE);
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            invoked = true;
        }
    }

    private static class FailingEmitter extends SseEmitter {
        FailingEmitter() {
            super(Long.MAX_VALUE);
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            throw new IOException("disconnect");
        }
    }
}

