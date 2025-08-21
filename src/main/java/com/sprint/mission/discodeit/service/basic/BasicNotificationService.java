package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.NotificationDto;
import com.sprint.mission.discodeit.entity.Notification;
import com.sprint.mission.discodeit.exception.ForbiddenException;
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
        Notification notification = new Notification(receiverId, title, content);
        notificationRepository.save(notification);
        return notificationMapper.toDto(notification);
    }

    @Override
    public List<NotificationDto> findAllByReceiverId(UUID receiverId) {
        return notificationRepository.findAllByReceiverIdOrderByCreatedAtDesc(receiverId)
            .stream()
            .map(notificationMapper::toDto)
            .toList();
    }

    @Transactional
    @Override
    public void delete(UUID notificationId, UUID receiverId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new NotificationNotFoundException());

        if (!notification.getReceiverId().equals(receiverId)) {
            throw new ForbiddenException();
        }

        notificationRepository.delete(notification);
    }
}
