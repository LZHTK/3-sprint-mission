package com.sprint.mission.discodeit.event;

import com.sprint.mission.discodeit.dto.data.NotificationDto;
import com.sprint.mission.discodeit.mapper.NotificationMapper;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.service.NotificationService;
import com.sprint.mission.discodeit.service.SseService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${server.instance-id:default}")
    private String instanceId;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(MessageCreateEvent event) {
        // 분산환경에서는 첫 번째 인스턴스만 전체 처리
        if (isDistributedEnvironment() && !isPrimaryInstance()) {
            log.debug("[알림 처리 스킵] 인스턴스: {}, 이벤트: MessageCreate", instanceId);
            return;
        }

        log.info("[메시지 생성 이벤트 처리 시작] 인스턴스: {}, 메시지 ID: {}", instanceId, event.messageId());

        try {
            // 알림 활성화된 사용자들 조회
            var notificationEnabledStatuses = readStatusRepository
                .findAllByChannelIdAndNotificationEnabledTrue(event.channelId());

            for (var readStatus : notificationEnabledStatuses) {
                // 메시지 작성자는 자기에게 알림 보내지 않음
                if (!readStatus.getUser().getId().equals(event.authorId())) {
                    // 1. 알림 생성
                    NotificationDto notification = notificationService.create(
                        readStatus.getUser().getId(),
                        event.channelName() + " 채널에 새 메시지가 도착했습니다.",
                        event.content()
                    );

                    // 2. SSE 전송 ( 같은 인스턴스에서 바로 처리 )
                    sseService.send(
                        List.of(readStatus.getUser().getId()),
                        "notifications.new",
                        notification
                    );

                    log.info("[새 메시지 알림 완료] 수신자: {}, 인스턴스: {}, 알림ID: {}",
                        readStatus.getUser().getId(), instanceId, notification.id());
                }
            }
        } catch (Exception e) {
            log.error("[메시지 생성 이벤트 처리 실패] 인스턴스: {}, 메시지 ID: {}",
                instanceId, event.messageId(), e);
        }
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(RoleUpdatedEvent event) {
        // 분산환경에서는 첫 번째 인스턴스만 전체 처리
        if (isDistributedEnvironment() && !isPrimaryInstance()) {
            log.debug("[알림 처리 스킵] 인스턴스: {}, 이벤트: RoleUpdated", instanceId);
            return;
        }

        log.info("[권한 변경 이벤트 처리 시작] 인스턴스: {}, 사용자 ID: {}", instanceId, event.userId());

        try {
            String title = "권한이 변경되었습니다.";
            String content = String.format("%s -> %s", event.oldRole(), event.newRole());

            // 1. 알림 생성
            NotificationDto notification = notificationService.create(event.userId(), title, content);

            // 2. SSE 전송 ( 같은 인스턴스에서 바로 처리 )
            sseService.send(
                List.of(event.userId()),
                "notifications.role_changed",
                notification
            );

            log.info("[권한 변경 알림 완료] 수신자: {}, 인스턴스: {}, 알림ID: {}",
                event.userId(), instanceId, notification.id());
        } catch (Exception e) {
            log.error("[권한 변경 이벤트 처리 실패] 인스턴스: {}, 사용자 ID: {}",
                instanceId, event.userId(), e);
        }
    }

    private boolean isDistributedEnvironment() {
        return !"default".equals(instanceId);
    }

    private boolean isPrimaryInstance() {
        return "backend-1".equals(instanceId);
    }
}