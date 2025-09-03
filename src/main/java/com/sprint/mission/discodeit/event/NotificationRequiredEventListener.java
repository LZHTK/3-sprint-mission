package com.sprint.mission.discodeit.event;

import com.sprint.mission.discodeit.dto.data.NotificationDto;
import com.sprint.mission.discodeit.entity.Notification;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.mapper.NotificationMapper;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.service.NotificationService;
import com.sprint.mission.discodeit.service.SseService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRequiredEventListener {

    private final NotificationService notificationService;
    private final ReadStatusRepository readStatusRepository;
    private final SseService sseService;
    private final NotificationMapper notificationMapper;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(MessageCreateEvent event) {
        log.info("비동기 메시지 알림 처리 시작 - 스레드 : {}, 채널 : {} ",
            Thread.currentThread().getName(), event.channelId());

        List<ReadStatus> readStatuses = readStatusRepository
            .findAllByChannelIdAndNotificationEnabledTrue(event.channelId());

        readStatuses.stream()
            .filter(readStatus -> !readStatus.getUser().getId().equals(event.authorId()))
            .forEach(readStatus -> {
                String title = String.format("%s (#%s)", event.authorUsername(), event.channelName());
                String content = event.content();

                log.debug("비동기 메시지 알림 생성 - 수신자 : {}, 제목 : {}", readStatus.getUser().getUsername(), title);

                // 알림 생성
                NotificationDto notification = notificationService.create(readStatus.getUser().getId(), title, content);

                // SSE 전송
                try {
                    sseService.send(
                        List.of(readStatus.getUser().getId()),
                        "notifications.created",
                        notification
                    );
                    log.debug("SSE 메시지 알림 전송 성공 : userId = {}", readStatus.getUser().getId());
                } catch (Exception e) {
                    log.error("SSE 메시지 알림 전송 실패 : userId = {}, error = {}",
                        readStatus.getUser().getId(), e.getMessage());
                }
            });

        log.info("비동기 메시지 알림 처리 완료 - 스레드 : {}, 채널 : {}, 알림 대상 : {} ",
            Thread.currentThread().getName(), event.channelId(), (int) readStatuses.stream()
                .filter(readStatus -> !readStatus.getUser().getId().equals(event.authorId())).count());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(RoleUpdatedEvent event) {
        log.info("비동기 권한 변경 알림 처리 시작 - 스레드 : {}, 사용자 : {}",
            Thread.currentThread().getName(), event.userId());

        String title = "권한이 변경되었습니다.";
        String content = String.format("%s -> %s", event.oldRole(), event.newRole());

        // 알림 생성
        NotificationDto notification = notificationService.create(event.userId(), title, content);

        // SSE 전송
        try {
            sseService.send(
                List.of(event.userId()),
                "notifications.created",
                notification
            );
            log.debug("SSE 권한 변경 알림 전송 성공: userId = {}", event.userId());
        } catch (Exception e) {
            log.error("SSE 권한 변경 알림 전송 실패: userId = {}, error = {}",
                event.userId(), e.getMessage());
        }

        log.info("비동기 권한 변경 알림 처리 완료 - 스레드 : {}, 사용자 : {}",
            Thread.currentThread().getName(), event.userId());
    }
}