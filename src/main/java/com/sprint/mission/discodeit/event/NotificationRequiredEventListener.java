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
                String title = String.format("%s (#%s)", event.authorUsername(), event.channelName());
                String content = event.content();
                notificationService.create(readStatus.getUser().getId(), title, content);
            });
    }

    @TransactionalEventListener
    public void on(RoleUpdatedEvent event) {
        String title = "권한이 변경되었습니다.";
        String content = String.format("%s -> %s", event.oldRole(), event.newRole());
        notificationService.create(event.userId(), title, content);
    }
}
