package com.sprint.mission.discodeit.event;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.mission.discodeit.dto.data.NotificationDto;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.service.NotificationService;
import com.sprint.mission.discodeit.service.SseService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class NotificationRequiredEventListenerTest {

    @Mock private NotificationService notificationService;
    @Mock private ReadStatusRepository readStatusRepository;
    @Mock private SseService sseService;
    @InjectMocks private NotificationRequiredEventListener listener;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(listener, "instanceId", "test-node");
    }

    @Test
    @DisplayName("메시지 생성 이벤트 발생 시 작성자를 제외한 사용자에게만 알림을 발송한다")
    void onMessageCreate_작성자제외알림() {
        // given: PRIVATE 채널에서 알림 허용된 사용자가 한 명 존재
        UUID channelId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        ReadStatus readStatus = createReadStatus(receiverId, channelId);
        given(readStatusRepository.findAllByChannelIdAndNotificationEnabledTrue(channelId))
            .willReturn(List.of(readStatus));
        NotificationDto notificationDto = new NotificationDto(
            UUID.randomUUID(), Instant.now(), receiverId, "title", "body");
        given(notificationService.create(eq(receiverId), anyString(), anyString()))
            .willReturn(notificationDto);
        MessageCreateEvent event = new MessageCreateEvent(
            UUID.randomUUID(), channelId, authorId, "writer", "개발 채널", "새 메시지");

        // when: 메시지 이벤트 처리
        listener.on(event);

        // then: NotificationService + SseService가 receiver에게만 호출됨
        then(notificationService).should()
            .create(eq(receiverId), contains("개발 채널"), eq("새 메시지"));
        then(sseService).should()
            .send(eq(List.of(receiverId)), eq("notifications.new"), eq(notificationDto));
    }

    @Test
    @DisplayName("역할 변경 이벤트는 대상 사용자 단일 SSE 이벤트로 전송된다")
    void onRoleUpdated_단일사용자전송() {
        // given
        UUID userId = UUID.randomUUID();
        NotificationDto notificationDto = new NotificationDto(
            UUID.randomUUID(), Instant.now(), userId, "title", "content");
        given(notificationService.create(eq(userId), anyString(), anyString()))
            .willReturn(notificationDto);
        RoleUpdatedEvent event = new RoleUpdatedEvent(userId, Role.USER, Role.ADMIN);

        // when
        listener.on(event);

        // then
        then(notificationService).should()
            .create(eq(userId), contains("변경"), contains("->"));
        then(sseService).should()
            .send(eq(List.of(userId)), eq("notifications.role_changed"), eq(notificationDto));
    }

    private ReadStatus createReadStatus(UUID userId, UUID channelId) {
        User user = new User("receiver", "receiver@sprint.io", "password", null);
        Channel channel = new Channel(ChannelType.PRIVATE, "채널", "설명");
        ReadStatus readStatus = new ReadStatus(user, channel, Instant.now());
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(channel, "id", channelId);
        return readStatus;
    }
}

