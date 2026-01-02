package com.sprint.mission.discodeit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.discodeit.dto.data.NotificationDto;
import com.sprint.mission.discodeit.service.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc(addFilters = false)
@Import(CacheTestControllerTest.TestConfig.class)
@DisplayName("CacheTestController WebMvc 테스트")
public class CacheTestControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("userNotifications");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    @DisplayName("테스트 알림 생성 시 서비스 위임과 반환값을 확인한다")
    void createTestNotification_shouldDelegateToService() throws Exception {
        UUID receiverId = UUID.randomUUID();
        NotificationDto dto = new NotificationDto(UUID.randomUUID(), Instant.now(), receiverId, "title", "content");
        given(notificationService.create(receiverId, "title", "content")).willReturn(dto);

        String response = mockMvc.perform(post("/api/test/cache/notifications")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("receiverId", receiverId.toString())
                .param("title", "title")
                .param("content", "content"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(response).contains(dto.id().toString());
        then(notificationService).should().create(receiverId, "title", "content");
    }

    @Test
    @DisplayName("알림 다중 조회 시 지정된 횟수만큼 서비스가 호출된다")
    void getNotificationsMultipleTimes_shouldInvokeServiceRepeatedly() throws Exception {
        UUID receiverId = UUID.randomUUID();
        NotificationDto dto = new NotificationDto(UUID.randomUUID(), Instant.now(), receiverId, "t", "c");
        given(notificationService.findAllByReceiverId(receiverId)).willReturn(List.of(dto));

        mockMvc.perform(get("/api/test/cache/notifications/{id}", receiverId).param("times", "2"))
            .andExpect(status().isOk());

        then(notificationService).should(org.mockito.Mockito.times(2)).findAllByReceiverId(receiverId);
    }

    @Test
    @DisplayName("캐시 통계 조회 시 캐시 유무를 알려준다")
    void getCacheStats_shouldDescribeCachePresence() throws Exception {
        Cache cache = cacheManager.getCache("userNotifications");
        assertThat(cache).isNotNull();

        String response = mockMvc.perform(get("/api/test/cache/stats"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(response).contains("Cache found");
    }

    @Test
    @DisplayName("캐시 비우기 요청 시 ValueWrapper 여부에 따라 로그를 남긴다")
    void clearCache_shouldEvictReceiverEntry() throws Exception {
        Cache cache = cacheManager.getCache("userNotifications");
        assertThat(cache).isNotNull();
        UUID receiverId = UUID.randomUUID();
        cache.put(receiverId, "value");

        mockMvc.perform(delete("/api/test/cache/clear/{id}", receiverId))
            .andExpect(status().isOk());

        assertThat(cache.get(receiverId)).isNull();
    }

    @Test
    @DisplayName("디버그 엔드포인트는 캐시 매니저 정보를 노출한다")
    void debugCache_shouldExposeCacheMetadata() throws Exception {
        cacheManager.getCache("userNotifications");

        String response = mockMvc.perform(get("/api/test/cache/debug"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(response).contains("userNotifications");
    }

    @Test
    @DisplayName("Redis 상태 체크는 테스트 키를 쓰고 제거한다")
    void getRedisStatus_shouldWriteAndClearTestEntry() throws Exception {
        Cache cache = cacheManager.getCache("userNotifications");
        assertThat(cache).isNotNull();

        String response = mockMvc.perform(get("/api/test/cache/redis-status"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(response).contains("connected");
        assertThat(cache.get("connection-test")).isNull();
    }
}
