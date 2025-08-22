package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.NotificationDto;
import com.sprint.mission.discodeit.entity.Notification;
import com.sprint.mission.discodeit.exception.ForbiddenException;
import com.sprint.mission.discodeit.exception.notification.NotificationAccessDeniedException;
import com.sprint.mission.discodeit.exception.notification.NotificationNotFoundException;
import com.sprint.mission.discodeit.mapper.NotificationMapper;
import com.sprint.mission.discodeit.repository.NotificationRepository;
import com.sprint.mission.discodeit.service.NotificationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Not;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class BasicNotificationService implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Transactional
    @Override
    public NotificationDto create(UUID receiverId, String title, String content) {
        log.info("[알림 생성 시도] 수신자: {}, 제목: {}", receiverId, title);

        Notification notification = new Notification(receiverId, title, content);
        notificationRepository.save(notification);


        log.info("[알림 생성 성공] ID: {}, 수신자: {}", notification.getId(), receiverId);
        return notificationMapper.toDto(notification);
    }

    @Transactional(readOnly = true)
    @Override
    public List<NotificationDto> findAllByReceiverId(UUID receiverId) {
        log.info("[알림 목록 조회] 수신자: {}", receiverId);

        List<NotificationDto> notifications = notificationRepository.findAllByReceiverIdOrderByCreatedAtDesc(receiverId)
            .stream()
            .map(notificationMapper::toDto)
            .toList();

        log.info("[알림 목록 조회 완료] 수신자: {}, 알림 수: {}개", receiverId, notifications.size());
        return notifications;
    }

    @Transactional
    @Override
    public void delete(UUID notificationId, UUID receiverId) {
        log.info("[알림 삭제 시도] ID: {}, 요청자: {}", notificationId, receiverId);

        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> {
                log.error("[알림 삭제 실패] 알림을 찾을 수 없습니다. ID: {}", notificationId);
                return new NotificationNotFoundException();
            });

        // 권한 검증: 본인의 알림만 삭제 가능
        if (!notification.getReceiverId().equals(receiverId)) {
            log.error("[알림 삭제 실패] 권한이 없습니다. 알림 ID: {}, 소유자: {}, 요청자: {}",
                notificationId, notification.getReceiverId(), receiverId);
            throw new NotificationAccessDeniedException();
        }

        notificationRepository.delete(notification);
        log.info("[알림 삭제 성공] ID: {}, 삭제자: {}", notificationId, receiverId);
    }
}
