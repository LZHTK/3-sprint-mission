package com.sprint.mission.discodeit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@DisplayName("SseEmitterRepository 단위 테스트")
public class SseEmitterRepositoryTest {

    private final SseEmitterRepository repository = new SseEmitterRepository();

    @Test
    @DisplayName("저장한 emitter는 사용자별로 보관된다")
    void save_shouldStoreEmittersPerUser() {
        // given
        UUID receiverId = UUID.randomUUID();
        SseEmitter emitter = new SseEmitter();

        // when
        repository.save(receiverId, emitter);

        // then
        List<SseEmitter> emitters = repository.findAllByReceiverId(receiverId);
        assertThat(emitters).containsExactly(emitter);
    }

    @Test
    @DisplayName("Emitter 삭제 시 리스트가 비면 키도 제거된다")
    void delete_shouldRemoveEmitterAndCleanupKey() {
        // given
        UUID receiverId = UUID.randomUUID();
        SseEmitter emitter = new SseEmitter();
        repository.save(receiverId, emitter);

        // when
        repository.delete(receiverId, emitter);

        // then
        assertThat(repository.findAllByReceiverId(receiverId)).isEmpty();
        assertThat(repository.findAll()).doesNotContainKey(receiverId);
    }
}
