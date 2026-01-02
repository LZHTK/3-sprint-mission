package com.sprint.mission.discodeit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.discodeit.dto.data.SseMessage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SseMessageRepository 단위 테스트")
public class SseMessageRepositoryTest {

    private final SseMessageRepository repository = new SseMessageRepository();

    @Test
    @DisplayName("MAX_SIZE를 초과하면 가장 오래된 이벤트를 제거한다")
    void save_shouldTrimOldestMessagesWhenExceedingMaxSize() {
        // given
        UUID firstInsertedId = UUID.randomUUID();
        repository.save(new SseMessage(firstInsertedId, "event-0", "payload-0", Instant.now()));

        // when
        for (int i = 1; i <= 1004; i++) {
            UUID id = UUID.randomUUID();
            repository.save(new SseMessage(id, "event-" + i, "payload-" + i, Instant.now()));
        }

        // then
        List<SseMessage> messages = repository.findEventsAfter(null);
        assertThat(messages).hasSize(1000);
        assertThat(messages).noneMatch(message -> message.id().equals(firstInsertedId));
    }

    @Test
    @DisplayName("특정 ID 이후의 이벤트만 조회한다")
    void findEventsAfter_shouldReturnMessagesAfterGivenId() {
        // given
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        UUID thirdId = UUID.randomUUID();
        repository.save(new SseMessage(firstId, "first", "one", Instant.now()));
        repository.save(new SseMessage(secondId, "second", "two", Instant.now()));
        repository.save(new SseMessage(thirdId, "third", "three", Instant.now()));

        // when
        List<SseMessage> result = repository.findEventsAfter(secondId);

        // then
        assertThat(result).extracting(SseMessage::id).containsExactly(thirdId);
        assertThat(repository.findEventsAfter(null)).hasSize(3);
    }
}
