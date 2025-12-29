package com.sprint.mission.discodeit.storage.local;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public class LocalBinaryContentStorageTest {

    @TempDir Path tempDir;
    private LocalBinaryContentStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalBinaryContentStorage(tempDir);
        storage.init();
    }

    @Test
    @DisplayName("put과 get을 통해 파일을 정상적으로 저장/조회한다")
    void putAndGet_success() throws Exception {
        // given: 저장할 바이트 배열
        UUID id = UUID.randomUUID();
        byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);

        // when: put 후 get으로 다시 읽음
        storage.put(id, payload);
        InputStream result = storage.get(id);

        // then: 저장한 내용과 동일하게 반환
        assertThat(result.readAllBytes()).isEqualTo(payload);
    }

    @Test
    @DisplayName("같은 ID로 두 번 put을 호출하면 IllegalArgumentException이 발생한다")
    void put_duplicateKey_throws() throws Exception {
        // given: 동일 ID로 이미 한 번 저장된 상태
        UUID id = UUID.randomUUID();
        storage.put(id, "first".getBytes(StandardCharsets.UTF_8));

        // when: 두 번째 put 시도
        // then: 중복 예외
        assertThatThrownBy(() -> storage.put(id, "second".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("파일이 존재하지 않으면 get은 NoSuchElementException을 던진다")
    void get_missingFile_throws() {
        // given: 아무것도 저장되지 않은 ID
        UUID id = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() -> storage.get(id))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("download는 파일 메타데이터에 맞는 ResponseEntity를 반환한다")
    void download_returnsResponseEntity() throws Exception {
        // given: 파일 저장 및 DTO 준비
        UUID id = UUID.randomUUID();
        byte[] payload = "file-data".getBytes(StandardCharsets.UTF_8);
        storage.put(id, payload);
        BinaryContentDto dto = new BinaryContentDto(
            id, "file.txt", (long) payload.length, "text/plain", BinaryContentStatus.SUCCESS);

        // when
        ResponseEntity<Resource> response = storage.download(dto);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
            .isEqualTo("attachment; filename=\"file.txt\"");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getInputStream().readAllBytes()).isEqualTo(payload);
    }
}
