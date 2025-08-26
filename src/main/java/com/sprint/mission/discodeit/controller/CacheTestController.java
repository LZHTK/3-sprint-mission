package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.data.NotificationDto;
import com.sprint.mission.discodeit.service.NotificationService;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/test/cache")
@Slf4j
public class CacheTestController {

    private final NotificationService notificationService;
    private final CacheManager cacheManager;

    @PostMapping("/notifications")
    public ResponseEntity<NotificationDto> createTestNotification(
        @RequestParam UUID receiverId,
        @RequestParam String title,
        @RequestParam String content) {

        log.info("[CACHE-TEST] 테스트 알림 생성 - 수신자 : {} ", receiverId);
        NotificationDto notification = notificationService.create(receiverId, title, content);
        return ResponseEntity.ok().body(notification);
    }

    @GetMapping("/notifications/{receiverId}")
    public ResponseEntity<List<NotificationDto>> getNotificationsMultipleTimes(
        @PathVariable UUID receiverId,
        @RequestParam(defaultValue = "3") int times) {

        log.info("[CACHE-TEST] 캐시 테스트 시작 - {}번 연속 호출", times);

        List<NotificationDto> result = null;
        for (int i = 1; i <= times; i++) {
            log.info("[CACHE-TEST] {}번째 호출 시작", i);
            long startTime = System.currentTimeMillis();

            result = notificationService.findAllByReceiverId(receiverId);

            long endTime = System.currentTimeMillis();
            log.info("[CACHE-TEST] {}번째 호출 완료 - 응답시간: {}ms", i, (endTime - startTime));
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<String> getCacheStats() {
        var cache = cacheManager.getCache("userNotifications");
        if (cache != null) {
            log.info("[CACHE-STATS] 캐시 상태 조회");
            return ResponseEntity.ok("Cache found: userNotifications");
        }
        return ResponseEntity.ok("No cache found");
    }

    @DeleteMapping("/clear/{receiverId}")
    public ResponseEntity<String> clearCache(@PathVariable UUID receiverId) {
        var cache = cacheManager.getCache("userNotifications");
        if (cache != null) {
            boolean hadValue = cache.get(receiverId) != null;
            cache.evict(receiverId);
            log.info("[CACHE-CLEAR] 캐시 수동 삭제 - 사용자: {}, 기존 값 존재: {}", receiverId, hadValue);
            return ResponseEntity.ok("Cache cleared for user: " + receiverId + ", had value: " + hadValue);
        }
        return ResponseEntity.ok("No cache found");
    }

    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debugCache() {
        Map<String, Object> info = new HashMap<>();

        // 기존 코드...
        info.put("cacheManagerType", cacheManager.getClass().getSimpleName());
        Collection<String> cacheNames = cacheManager.getCacheNames();
        info.put("cacheNames", cacheNames);

        // 3. 각 캐시의 상태 및 통계 확인
        Map<String, Object> cacheStatus = new HashMap<>();
        for (String cacheName : cacheNames) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Map<String, Object> cacheInfo = new HashMap<>();
                cacheInfo.put("exists", true);
                cacheInfo.put("nativeCache", cache.getNativeCache().getClass().getSimpleName());

                // Caffeine 네이티브 캐시에서 통계 확인
                Object nativeCache = cache.getNativeCache();
                if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache) {
                    var caffeineCache = (com.github.benmanes.caffeine.cache.Cache<?, ?>) nativeCache;
                    try {
                        var stats = caffeineCache.stats();
                        cacheInfo.put("statsEnabled", true);
                        cacheInfo.put("hitCount", stats.hitCount());
                        cacheInfo.put("missCount", stats.missCount());
                        cacheInfo.put("requestCount", stats.requestCount());
                        cacheInfo.put("hitRate", stats.hitRate());
                    } catch (Exception e) {
                        cacheInfo.put("statsEnabled", false);
                        cacheInfo.put("statsError", e.getMessage());
                    }
                }
                cacheStatus.put(cacheName, cacheInfo);
            } else {
                cacheStatus.put(cacheName, Map.of("exists", false));
            }
        }
        info.put("cacheStatus", cacheStatus);

        return ResponseEntity.ok(info);
    }
}
