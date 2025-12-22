package com.sprint.mission.discodeit.redis;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisLockProviderTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @InjectMocks private RedisLockProvider redisLockProvider;

    @Test
    @DisplayName("락이 비어있으면 성공적으로 획득한다")
    void acquireLock_성공() {
        // given: Redis setIfAbsent가 true를 반환
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class))).willReturn(true);

        // when: acquireLock 호출
        redisLockProvider.acquireLock("channel-1");

        // then: setIfAbsent가 lock:channel-1 키로 호출됨
        then(valueOperations).should()
            .setIfAbsent(eq("lock:channel-1"), any(), any(Duration.class));
    }

    @Test
    @DisplayName("이미 잠겨있으면 예외를 던진다")
    void acquireLock_실패() {
        // given: setIfAbsent가 false 반환
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class))).willReturn(false);

        // when / then: 예외 발생
        assertThatThrownBy(() -> redisLockProvider.acquireLock("duplicated"))
            .isInstanceOf(RedisLockProvider.RedisLockAcquisitionException.class);
    }

    @Test
    @DisplayName("해제 중 예외가 발생해도 로그만 찍고 넘어간다")
    void releaseLock_예외무시() {
        // given: redisTemplate.delete가 예외 던짐
        doThrow(new RuntimeException("delete fail")).when(redisTemplate).delete("lock:oops");

        // when: releaseLock 호출
        redisLockProvider.releaseLock("oops");

        // then: delete는 시도되지만 예외는 전파되지 않음
        then(redisTemplate).should().delete("lock:oops");
    }
}
