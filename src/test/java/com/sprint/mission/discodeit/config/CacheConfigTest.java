package com.sprint.mission.discodeit.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

public class CacheConfigTest {

    private final CacheConfig cacheConfig = new CacheConfig();

    @Test
    @DisplayName("캐시 설정은 prefix · TTL · null 캐싱 여부를 명시한다")
    void redisCacheConfiguration_shouldCustomizePrefixTtlAndNullHandling() {
        // given
        ObjectMapper objectMapper = new ObjectMapper();

        // when
        RedisCacheConfiguration configuration = cacheConfig.redisCacheConfiguration(objectMapper);

        // then
        assertThat(configuration.getTtl()).isEqualTo(Duration.ofSeconds(600));
        assertThat(configuration.getKeyPrefixFor("channels")).isEqualTo("discodeit:channels::");
        assertThat(configuration.getAllowCacheNullValues()).isFalse();
    }
}
