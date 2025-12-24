package com.sprint.mission.discodeit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.sprint.mission.discodeit.dto.data.NotificationDto;
import com.sprint.mission.discodeit.entity.Notification;
import com.sprint.mission.discodeit.exception.notification.NotificationAccessDeniedException;
import com.sprint.mission.discodeit.exception.notification.NotificationNotFoundException;
import com.sprint.mission.discodeit.mapper.NotificationMapper;
import com.sprint.mission.discodeit.repository.NotificationRepository;
import com.sprint.mission.discodeit.service.basic.BasicNotificationService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
class BasicNotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationMapper notificationMapper;
    @Mock private CacheManager cacheManager;
    @Mock private Cache cache;

    @InjectMocks private BasicNotificationService notificationService;

    @Test
    @DisplayName("알림 생성 시 저장과 캐시 무효화가 수행된다")
    void create_캐시무효화() {
        // given
        UUID receiverId = UUID.randomUUID();
        NotificationDto dto = new NotificationDto(UUID.randomUUID(), Instant.now(), receiverId, "title", "content");
        given(notificationMapper.toDto(any(Notification.class))).willReturn(dto);
        given(cacheManager.getCache("userNotifications")).willReturn(cache);

        // when
        NotificationDto result = notificationService.create(receiverId, "title", "content");

        // then
        assertThat(result).isEqualTo(dto);
        then(notificationRepository).should().save(any(Notification.class));
        then(cache).should().evict(receiverId);
    }

    @Test
    @DisplayName("알림 목록 조회는 레포지토리 데이터를 DTO로 매핑한다")
    void findAllByReceiverId() {
        // given
        UUID receiverId = UUID.randomUUID();
        Notification entity = new Notification(receiverId, "title", "content");
        NotificationDto dto = new NotificationDto(UUID.randomUUID(), Instant.now(), receiverId, "title", "content");
        given(notificationRepository.findAllByReceiverIdOrderByCreatedAtDesc(receiverId)).willReturn(List.of(entity));
        given(notificationMapper.toDto(entity)).willReturn(dto);

        // when
        List<NotificationDto> result = notificationService.findAllByReceiverId(receiverId);

        // then
        assertThat(result).containsExactly(dto);
        then(notificationRepository).should().findAllByReceiverIdOrderByCreatedAtDesc(receiverId);
    }

    @Test
    @DisplayName("다른 사용자의 알림을 삭제하면 예외가 발생한다")
    void delete_권한없음() {
        // given
        UUID notificationId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        UUID currentUser = UUID.randomUUID();
        Notification otherNotification = new Notification(otherUser, "title", "content");
        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(otherNotification));

        // when / then
        assertThatThrownBy(() -> notificationService.delete(notificationId, currentUser))
            .isInstanceOf(NotificationAccessDeniedException.class);
        then(notificationRepository).should(never()).delete(any(Notification.class));
    }

    @Test
    @DisplayName("존재하지 않는 알림을 삭제하면 NotFound 예외가 발생한다")
    void delete_존재하지않음() {
        // given
        UUID notificationId = UUID.randomUUID();
        given(notificationRepository.findById(notificationId)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> notificationService.delete(notificationId, UUID.randomUUID()))
            .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    @DisplayName("본인의 알림을 삭제하면 레포지토리와 캐시가 정리된다")
    void delete_성공() {
        // given
        UUID receiverId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification(receiverId, "title", "content");
        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

        // when
        notificationService.delete(notificationId, receiverId);

        // then
        then(notificationRepository).should().delete(notification);
        // CacheEvict 애노테이션 동작은 프록시에서 처리되므로 직접 검증할 수 없지만,
        // 최소한 delete 로직이 예외 없이 마무리되는지를 확인한다.
    }
}
