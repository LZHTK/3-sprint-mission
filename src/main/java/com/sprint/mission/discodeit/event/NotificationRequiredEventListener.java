package com.sprint.mission.discodeit.event;

import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRequiredEventListener {

    private final NotificationService notificationService;
    private final ReadStatusRepository readStatusRepository;

    @TransactionalEventListener
    public void on(MessageCreateEvent event) {
        List<ReadStatus> readStatuses = readStatusRepository
            .findAllByChannelIdAndNotificationEnabledTrue(event.channelId());

        readStatuses.stream()
            .filter(readStatus -> !readStatus.getUser().getId().equals(event.authorId()))
            .forEach(readStatus -> {
                String channelDisplay = (event.channelName() != null && !event.channelName().trim().isEmpty())
                    ? "#" + event.channelName()
                    : "Private Message";

                String title = String.format("%s (#%s)", event.authorUsername(), event.channelName());
                String content = event.content();

                log.debug("메시지 알림 생성 - 수신자: {}, 제목: {}", readStatus.getUser().getId(), title);
                notificationService.create(readStatus.getUser().getId(), title, content);
            });

        log.info("메시지 알림 처리 완료 - 채널: {}, 알림 대상: {}명",
            event.channelId(), readStatuses.size() - 1); // 작성자 제외
    }

    @TransactionalEventListener
    public void on(RoleUpdatedEvent event) {
        String title = "권한이 변경되었습니다.";
        String content = String.format("%s -> %s", event.oldRole(), event.newRole());

        log.info("권한 변경 알림 생성 - 사용자: {}, 변경: {} -> {}",
            event.userId(), event.oldRole(), event.newRole());
        notificationService.create(event.userId(), title, content);
    }
}
