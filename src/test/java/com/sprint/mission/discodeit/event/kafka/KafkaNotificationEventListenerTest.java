package com.sprint.mission.discodeit.event.kafka;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.data.NotificationDto;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.event.MessageCreateEvent;
import com.sprint.mission.discodeit.event.RoleUpdatedEvent;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.service.NotificationService;
import com.sprint.mission.discodeit.service.SseService;
import java.time.Duration;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
public class KafkaNotificationEventListenerTest {

    @Mock private NotificationService notificationService;
    @Mock private ReadStatusRepository readStatusRepository;
    @Mock private SseService sseService;
    @Mock private ObjectMapper objectMapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks private KafkaNotificationEventListener listener;

    private void stubDedupPass() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.setIfAbsent(anyString(), any(), any(Duration.class))).willReturn(true);
    }

    @Test
    @DisplayName("MessageCreateEvent 를 수신하면 알림과 SSE 전송을 수행한다")
    void handleMessageCreateEvent() throws Exception {
        // given
        stubDedupPass();

        UUID channelId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID subscriberId = UUID.randomUUID();
        MessageCreateEvent event = new MessageCreateEvent(
            UUID.randomUUID(), channelId, authorId, "author", "dev", "hello");
        given(objectMapper.readValue("json", MessageCreateEvent.class)).willReturn(event);

        User subscriber = mock(User.class);
        given(subscriber.getId()).willReturn(subscriberId);

        var readStatus = mock(ReadStatus.class);
        given(readStatus.getUser()).willReturn(subscriber);
        given(readStatusRepository.findAllByChannelIdAndNotificationEnabledTrue(channelId))
            .willReturn(List.of(readStatus));

        NotificationDto notification = new NotificationDto(
            UUID.randomUUID(), Instant.now(), subscriberId, "title", event.content());
        given(notificationService.create(eq(subscriberId), any(), eq(event.content())))
            .willReturn(notification);

        // when
        listener.handleMessageCreateEvent("json");

        // then
        then(notificationService).should()
            .create(eq(subscriberId), any(), eq(event.content()));
        then(sseService).should()
            .send(eq(List.of(subscriberId)), eq("notifications.new"), eq(notification));
    }

    @Test
    @DisplayName("RoleUpdatedEvent 를 수신하면 단일 사용자에게 알림을 전송한다")
    void handleRoleUpdatedEvent() throws Exception {
        // given
        stubDedupPass();

        UUID userId = UUID.randomUUID();
        RoleUpdatedEvent event = new RoleUpdatedEvent(userId, Role.USER, Role.ADMIN);
        given(objectMapper.readValue("json", RoleUpdatedEvent.class)).willReturn(event);

        NotificationDto notification = new NotificationDto(
            UUID.randomUUID(), Instant.now(), userId, "role", "USER -> ADMIN");
        given(notificationService.create(eq(userId), any(), any())).willReturn(notification);

        // when
        listener.handleRoleUpdatedEvent("json");

        // then
        then(notificationService).should().create(eq(userId), any(), any());
        then(sseService).should()
            .send(eq(List.of(userId)), eq("notifications.role_changed"), eq(notification));
    }

    @Test
    @DisplayName("JSON 역직렬화 실패 시 예외는 삼키고 종료한다")
    void handleMessageCreateEvent_jsonError() throws Exception {
        given(objectMapper.readValue("bad", MessageCreateEvent.class))
            .willThrow(new RuntimeException("boom"));

        ThrowingCallable when = () -> listener.handleMessageCreateEvent("bad");
        assertThatCode(when).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("메시지 작성자에게는 알림과 SSE 전송을 하지 않는다")
    void handleMessageCreateEvent_skipsAuthor() throws Exception {
        // given
        stubDedupPass();

        UUID channelId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        MessageCreateEvent event = new MessageCreateEvent(
            UUID.randomUUID(), channelId, authorId, "me", "dev", "self");
        given(objectMapper.readValue("json", MessageCreateEvent.class)).willReturn(event);

        User author = mock(User.class);
        given(author.getId()).willReturn(authorId);
        ReadStatus status = mock(ReadStatus.class);
        given(status.getUser()).willReturn(author);
        given(readStatusRepository.findAllByChannelIdAndNotificationEnabledTrue(channelId))
            .willReturn(List.of(status));

        // when
        listener.handleMessageCreateEvent("json");

        // then
        then(notificationService).shouldHaveNoInteractions();
        then(sseService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("RoleUpdatedEvent 역직렬화 실패 시 예외는 전파하지 않는다")
    void handleRoleUpdatedEvent_jsonError() throws Exception {
        given(objectMapper.readValue("bad", RoleUpdatedEvent.class))
            .willThrow(new RuntimeException("boom"));

        ThrowingCallable when = () -> listener.handleRoleUpdatedEvent("bad");
        assertThatCode(when).doesNotThrowAnyException();
        then(notificationService).shouldHaveNoInteractions();
        then(sseService).shouldHaveNoInteractions();
    }
}
