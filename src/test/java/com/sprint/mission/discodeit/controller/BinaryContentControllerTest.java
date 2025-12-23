package com.sprint.mission.discodeit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import com.sprint.mission.discodeit.service.BinaryContentService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BinaryContentControllerTest {

    @Mock private BinaryContentService binaryContentService;
    @Mock private BinaryContentStorage binaryContentStorage;

    @InjectMocks private BinaryContentController controller;

    private UUID binaryContentId;
    private BinaryContentDto dto;

    @BeforeEach
    void setUp() {
        binaryContentId = UUID.randomUUID();
        dto = new BinaryContentDto(binaryContentId, "sample.txt", 4L, "text/plain",
            BinaryContentStatus.SUCCESS);
    }

    @Test
    @DisplayName("download API는 스토리지에서 읽은 바이트를 그대로 반환한다")
    void download_성공() throws Exception {
        // given: 파일 메타와 InputStream 준비
        byte[] payload = "test".getBytes(StandardCharsets.UTF_8);
        given(binaryContentService.find(binaryContentId)).willReturn(dto);
        given(binaryContentStorage.get(binaryContentId))
            .willReturn(new ByteArrayInputStream(payload));

        // when: 다운로드 호출
        var response = controller.download(binaryContentId);

        // then: 응답 헤더/바디 검증
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(payload);
        assertThat(response.getHeaders().getFirst("Content-Type")).isEqualTo("text/plain");
        then(binaryContentStorage).should().get(binaryContentId);
    }

    @Test
    @DisplayName("스토리지에서 IOException이 발생하면 500을 반환한다")
    void download_IO예외() throws Exception {
        // given
        given(binaryContentService.find(binaryContentId)).willReturn(dto);
        InputStream failingStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }
        };
        given(binaryContentStorage.get(binaryContentId)).willReturn(failingStream);

        // when
        var response = controller.download(binaryContentId);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNull();
        then(binaryContentStorage).should().get(binaryContentId);
    }

    @Test
    @DisplayName("여러 ID 조회 API는 서비스 결과를 그대로 전달한다")
    void findAllByIdIn_성공() {
        // given
        var anotherId = UUID.randomUUID();
        given(binaryContentService.findAllByIdIn(List.of(binaryContentId, anotherId)))
            .willReturn(List.of(dto));

        // when
        var response = controller.findAllByIdIn(List.of(binaryContentId, anotherId));

        // then
        assertThat(response.getBody()).containsExactly(dto);
        then(binaryContentService).should().findAllByIdIn(List.of(binaryContentId, anotherId));
    }
}
