package com.sprint.mission.discodeit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.sprint.mission.discodeit.dto.data.SseMessage;
import com.sprint.mission.discodeit.repository.SseEmitterRepository;
import com.sprint.mission.discodeit.repository.SseMessageRepository;
import com.sprint.mission.discodeit.service.basic.BasicSseService;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
@DisplayName("BasicSseService 단위 테스트")
public class BasicSseServiceTest {

    @Mock
    private SseEmitterRepository sseEmitterRepository;

    @Mock
    private SseMessageRepository sseMessageRepository;

    @InjectMocks
    private BasicSseService sseService;

    @Test
    @DisplayName("연결 시 emitter를 저장하고 이전 이벤트를 조회한다")
    void connect_shouldSaveEmitterAndLoadMissedEvents() {
        // given
        UUID receiverId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        List<SseMessage> missed = List.of(new SseMessage(UUID.randomUUID(), "messages", "payload", Instant.now()));
        given(sseMessageRepository.findEventsAfter(lastEventId)).willReturn(missed);

        // when
        SseEmitter emitter = sseService.connect(receiverId, lastEventId);

        // then
        assertThat(emitter).isNotNull();
        then(sseEmitterRepository).should().save(eq(receiverId), any(SseEmitter.class));
        then(sseMessageRepository).should().findEventsAfter(lastEventId);
    }

    @Test
    @DisplayName("특정 수신자 목록에 이벤트를 전송한다")
    void send_shouldPersistMessageAndDeliverToReceivers() throws Exception {
        // given
        UUID receiverId = UUID.randomUUID();
        SseEmitter emitter = org.mockito.Mockito.mock(SseEmitter.class);
        List<SseEmitter> emitters = new ArrayList<>(List.of(emitter));
        given(sseEmitterRepository.findAllByReceiverId(receiverId)).willReturn(emitters);
        Collection<UUID> receiverIds = List.of(receiverId);

        // when
        sseService.send(receiverIds, "message.created", "hello");

        // then
        then(sseMessageRepository).should().save(any(SseMessage.class));
        then(emitter).should().send(any(SseEmitter.SseEventBuilder.class));
        assertThat(emitters).hasSize(1);
    }

    @Test
    @DisplayName("브로드캐스트 시 실패한 emitter는 제거된다")
    void broadcast_shouldSendToAllEmittersAndRemoveFailures() throws Exception {
        // given
        UUID okReceiver = UUID.randomUUID();
        UUID failReceiver = UUID.randomUUID();
        SseEmitter successEmitter = org.mockito.Mockito.mock(SseEmitter.class);
        SseEmitter failingEmitter = org.mockito.Mockito.mock(SseEmitter.class);
        willThrow(new IOException("fail")).given(failingEmitter).send(any(SseEmitter.SseEventBuilder.class));
        ConcurrentMap<UUID, List<SseEmitter>> data = new ConcurrentHashMap<>();
        data.put(okReceiver, new ArrayList<>(List.of(successEmitter)));
        data.put(failReceiver, new ArrayList<>(List.of(failingEmitter)));
        given(sseEmitterRepository.findAll()).willReturn(data);

        // when
        sseService.broadcast("message.updated", "payload");

        // then
        then(sseMessageRepository).should().save(any(SseMessage.class));
        then(successEmitter).should().send(any(SseEmitter.SseEventBuilder.class));
        then(failingEmitter).should().send(any(SseEmitter.SseEventBuilder.class));
        assertThat(data.get(failReceiver)).isEmpty();
    }

    @Test
    @DisplayName("cleanUp은 ping 실패 emitter를 제거한다")
    void cleanUp_shouldPingEmittersAndRemoveFailures() throws Exception {
        // given
        UUID receiverId = UUID.randomUUID();
        SseEmitter aliveEmitter = org.mockito.Mockito.mock(SseEmitter.class);
        SseEmitter deadEmitter = org.mockito.Mockito.mock(SseEmitter.class);
        willThrow(new IOException("ping error")).given(deadEmitter).send(any(SseEmitter.SseEventBuilder.class));
        ConcurrentMap<UUID, List<SseEmitter>> data = new ConcurrentHashMap<>();
        data.put(receiverId, new ArrayList<>(List.of(aliveEmitter, deadEmitter)));
        given(sseEmitterRepository.findAll()).willReturn(data);

        // when
        sseService.cleanUp();

        // then
        assertThat(data.get(receiverId)).containsExactly(aliveEmitter);
    }

    @Test
    @DisplayName("sendHeartbeat는 실패 emitter를 제거한다")
    void sendHeartbeat_shouldRemoveEmittersWhenHeartbeatFails() throws Exception {
        // given
        UUID receiverId = UUID.randomUUID();
        SseEmitter aliveEmitter = org.mockito.Mockito.mock(SseEmitter.class);
        SseEmitter deadEmitter = org.mockito.Mockito.mock(SseEmitter.class);
        willThrow(new IOException("heartbeat error")).given(deadEmitter).send(any(SseEmitter.SseEventBuilder.class));
        ConcurrentMap<UUID, List<SseEmitter>> data = new ConcurrentHashMap<>();
        data.put(receiverId, new ArrayList<>(List.of(aliveEmitter, deadEmitter)));
        given(sseEmitterRepository.findAll()).willReturn(data);

        // when
        sseService.sendHeartbeat();

        // then
        assertThat(data.get(receiverId)).containsExactly(aliveEmitter);
    }
}
