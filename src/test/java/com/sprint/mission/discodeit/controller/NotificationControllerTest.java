package com.sprint.mission.discodeit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.mission.discodeit.dto.data.NotificationDto;
import com.sprint.mission.discodeit.security.DiscodeitUserDetails;
import com.sprint.mission.discodeit.service.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock private NotificationService notificationService;
    @Mock private Authentication authentication;
    @Mock private DiscodeitUserDetails userDetails;

    @InjectMocks private NotificationController controller;

    @Test
    @DisplayName("receiverId 파라미터가 없으면 현재 로그인 사용자를 사용한다")
    void getNotifications_현재사용자() {
        // given
        UUID currentUserId = UUID.randomUUID();
        var notification = new NotificationDto(
            UUID.randomUUID(), Instant.now(), currentUserId, "title", "content");
        given(authentication.getPrincipal()).willReturn(userDetails);
        given(userDetails.getUserId()).willReturn(currentUserId);
        given(notificationService.findAllByReceiverId(currentUserId)).willReturn(List.of(notification));

        // when
        ResponseEntity<List<NotificationDto>> response =
            controller.getNotifications(null, authentication);

        // then
        assertThat(response.getBody()).containsExactly(notification);
        then(notificationService).should().findAllByReceiverId(currentUserId);
    }

    @Test
    @DisplayName("receiverId를 지정하면 해당 사용자에게만 조회한다")
    void getNotifications_특정사용자() {
        // given
        UUID targetId = UUID.randomUUID();
        given(notificationService.findAllByReceiverId(targetId)).willReturn(List.of());

        // when
        controller.getNotifications(targetId, authentication);

        // then
        then(notificationService).should().findAllByReceiverId(targetId);
    }

    @Test
    @DisplayName("알림 삭제는 Authentication에서 추출한 사용자 ID로 검증한다")
    void deleteNotification_현재사용자() {
        // given
        UUID notificationId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        given(authentication.getPrincipal()).willReturn(userDetails);
        given(userDetails.getUserId()).willReturn(currentUserId);

        // when
        var response = controller.deleteNotification(notificationId, authentication);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        then(notificationService).should().delete(notificationId, currentUserId);
    }
}

