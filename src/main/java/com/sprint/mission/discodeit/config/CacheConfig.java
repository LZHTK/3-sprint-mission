package com.sprint.mission.discodeit.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Arrays;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Caffeine 설정
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(Duration.ofSeconds(600))
            .recordStats(); // 통계 활성화

        cacheManager.setCaffeine(caffeine);

        // 동적 캐시 생성 허용 (이 설정이 중요!)
        cacheManager.setAllowNullValues(false);

        // 캐시 이름들 명시적 설정
        cacheManager.setCacheNames(Arrays.asList(
            "userChannels",
            "userNotifications",
            "users"
        ));

        return cacheManager;
    }
}