package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.data.NotificationDto;
import com.sprint.mission.discodeit.security.DiscodeitUserDetails;
import com.sprint.mission.discodeit.service.NotificationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications(
        @RequestParam(required = false) UUID receiverId,
        Authentication authentication) {

        UUID targetReceiverId;
        if (receiverId != null) {
            targetReceiverId = receiverId;
        } else {
            DiscodeitUserDetails userDetails = (DiscodeitUserDetails) authentication.getPrincipal();
            targetReceiverId = userDetails.getUserId();
        }

        List<NotificationDto> notifications = notificationService.findAllByReceiverId(targetReceiverId);
        return ResponseEntity.ok().body(notifications);
    }

    @DeleteMapping(path = "{notificationId}")
    public ResponseEntity<Void> deleteNotification(
        @PathVariable UUID notificationId,
        Authentication authentication) {

        DiscodeitUserDetails userDetails = (DiscodeitUserDetails) authentication.getPrincipal();
        UUID currentUserId = userDetails.getUserId();

        notificationService.delete(notificationId, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
