package com.sprint.mission.discodeit.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

public class RedisConfigTest {

    private final RedisConfig redisConfig = new RedisConfig();

    @Test
    @DisplayName("RedisTemplate은 주입된 serializer를 key/value에 그대로 사용한다")
    void redisTemplate_shouldUseProvidedSerializer() {
        // given
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        GenericJackson2JsonRedisSerializer serializer =
            redisConfig.redisSerializer(new ObjectMapper());

        // when
        RedisTemplate<String, Object> template =
            redisConfig.redisTemplate(connectionFactory, serializer);

        // then
        assertThat(template.getValueSerializer()).isSameAs(serializer);
        assertThat(template.getHashValueSerializer()).isSameAs(serializer);
        assertThat(template.getKeySerializer()).isEqualTo(template.getStringSerializer());
    }

    @Test
    @DisplayName("redisSerializer는 타입 정보를 유지하는 JSON을 생성한다")
    void redisSerializer_shouldPreserveTypeMetadata() {
        // given
        ObjectMapper objectMapper = new ObjectMapper();

        // when
        GenericJackson2JsonRedisSerializer serializer =
            redisConfig.redisSerializer(objectMapper);

        // then
        byte[] jsonBytes = serializer.serialize(List.of(UUID.randomUUID(), "payload"));
        List<?> restored = (List<?>) serializer.deserialize(jsonBytes, List.class);

        assertThat(restored).hasSize(2);
        assertThat(restored.get(0)).isInstanceOf(UUID.class);
        assertThat(restored.get(1)).isEqualTo("payload");
    }
}
