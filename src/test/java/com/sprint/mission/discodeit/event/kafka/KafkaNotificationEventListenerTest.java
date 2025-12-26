package com.sprint.mission.discodeit.event.kafka;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.data.NotificationDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.event.MessageCreateEvent;
import com.sprint.mission.discodeit.event.RoleUpdatedEvent;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.service.NotificationService;
import com.sprint.mission.discodeit.service.SseService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KafkaNotificationEventListenerTest {

    @Mock private NotificationService notificationService;
    @Mock private ReadStatusRepository readStatusRepository;
    @Mock private SseService sseService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private KafkaNotificationEventListener listener;

    @Test
    @DisplayName("MessageCreateEvent 를 수신하면 알림과 SSE 전송을 수행한다")
    void handleMessageCreateEvent() throws Exception {
        // given: 채널 구독자와 JSON 페이로드 역직렬화가 준비됨
        UUID channelId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID subscriberId = UUID.randomUUID();
        MessageCreateEvent event = new MessageCreateEvent(
            UUID.randomUUID(), channelId, authorId, "author", "dev", "hello");
        given(objectMapper.readValue("json", MessageCreateEvent.class)).willReturn(event);

        User subscriber = mock(User.class);
        given(subscriber.getId()).willReturn(subscriberId);

        var readStatus = mock(com.sprint.mission.discodeit.entity.ReadStatus.class);
        given(readStatus.getUser()).willReturn(subscriber);
        given(readStatusRepository.findAllByChannelIdAndNotificationEnabledTrue(channelId))
            .willReturn(List.of(readStatus));

        NotificationDto notification = new NotificationDto(
            UUID.randomUUID(), Instant.now(), subscriberId, "title", event.content());
        given(notificationService.create(eq(subscriberId), any(), eq(event.content())))
            .willReturn(notification);

        // when: Kafka 메시지를 처리
        listener.handleMessageCreateEvent("json");

        // then: NotificationService와 SSE 전송이 모두 수행됨
        then(notificationService).should()
            .create(eq(subscriberId), any(), eq(event.content()));
        then(sseService).should()
            .send(eq(List.of(subscriberId)), eq("notifications.new"), eq(notification));
    }

    @Test
    @DisplayName("RoleUpdatedEvent 를 수신하면 단일 사용자에게 알림을 전송한다")
    void handleRoleUpdatedEvent() throws Exception {
        // given: 역할 변경 이벤트 JSON이 준비됨
        UUID userId = UUID.randomUUID();
        RoleUpdatedEvent event = new RoleUpdatedEvent(userId, Role.USER, Role.ADMIN);
        given(objectMapper.readValue("json", RoleUpdatedEvent.class)).willReturn(event);

        NotificationDto notification = new NotificationDto(
            UUID.randomUUID(), Instant.now(), userId, "role", "USER -> ADMIN");
        given(notificationService.create(eq(userId), any(), any())).willReturn(notification);

        // when: Kafka 메시지를 처리
        listener.handleRoleUpdatedEvent("json");

        // then: 알림 생성과 SSE 전송이 한 번씩 호출됨
        then(notificationService).should().create(eq(userId), any(), any());
        then(sseService).should()
            .send(eq(List.of(userId)), eq("notifications.role_changed"), eq(notification));
    }

    @Test
    @DisplayName("JSON 역직렬화 실패 시 예외를 잡고 로깅 후 종료한다")
    void handleMessageCreateEvent_jsonError() throws Exception {
        // given: ObjectMapper 가 예외를 던지도록 설정
        given(objectMapper.readValue("bad", MessageCreateEvent.class))
            .willThrow(new RuntimeException("boom"));

        // when: Kafka 메시지 처리 시도
        ThrowingCallable when = () -> listener.handleMessageCreateEvent("bad");

        // then: 예외가 외부로 던져지지 않음
        assertThatCode(when).doesNotThrowAnyException();
    }
}
