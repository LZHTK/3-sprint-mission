package com.sprint.mission.discodeit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import com.sprint.mission.discodeit.event.BinaryContentCreatedEvent;
import com.sprint.mission.discodeit.exception.binarycontent.BinaryContentNotFoundException;
import com.sprint.mission.discodeit.mapper.BinaryContentMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.service.basic.BasicBinaryContentService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("BasicBinaryContentService 단위 테스트")
public class BasicBinaryContentServiceTest {

    @Mock
    private BinaryContentRepository binaryContentRepository;

    @Mock
    private BinaryContentMapper binaryContentMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private SseService sseService;

    @InjectMocks
    private BasicBinaryContentService binaryContentService;

    @Test
    @DisplayName("파일 생성 성공 시 저장 및 이벤트 발행을 수행한다")
    void create_shouldPersistBinaryContentAndPublishEvent() {
        // given
        byte[] bytes = "hello-world".getBytes();
        BinaryContentCreateRequest request = new BinaryContentCreateRequest("sample.png", "image/png", bytes);
        BinaryContentDto expectedDto = new BinaryContentDto(
            UUID.randomUUID(), request.fileName(), (long) bytes.length, request.contentType(), BinaryContentStatus.PROCESSING
        );
        UUID generatedId = UUID.randomUUID();
        given(binaryContentRepository.save(any(BinaryContent.class))).willAnswer(invocation -> {
            BinaryContent entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", generatedId);
            return entity;
        });
        given(binaryContentMapper.toDto(any(BinaryContent.class))).willReturn(expectedDto);

        // when
        BinaryContentDto result = binaryContentService.create(request);

        // then
        assertThat(result).isEqualTo(expectedDto);
        ArgumentCaptor<BinaryContentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(BinaryContentCreatedEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        BinaryContentCreatedEvent event = eventCaptor.getValue();
        assertThat(event.binaryContentId()).isEqualTo(generatedId);
        assertThat(event.bytes()).isEqualTo(bytes);
        assertThat(event.fileName()).isEqualTo(request.fileName());
    }

    @Test
    @DisplayName("존재하는 파일 조회 시 DTO로 반환한다")
    void find_shouldReturnBinaryContentDtoWhenFound() {
        // given
        UUID binaryContentId = UUID.randomUUID();
        BinaryContent binaryContent = new BinaryContent("memo.txt", 11L, "text/plain");
        BinaryContentDto expectedDto = new BinaryContentDto(
            binaryContentId, "memo.txt", 11L, "text/plain", BinaryContentStatus.PROCESSING
        );
        given(binaryContentRepository.findById(binaryContentId)).willReturn(Optional.of(binaryContent));
        given(binaryContentMapper.toDto(binaryContent)).willReturn(expectedDto);

        // when
        BinaryContentDto result = binaryContentService.find(binaryContentId);

        // then
        assertThat(result).isEqualTo(expectedDto);
        then(binaryContentMapper).should().toDto(binaryContent);
    }

    @Test
    @DisplayName("존재하지 않는 파일 조회 시 예외를 던진다")
    void find_shouldThrowExceptionWhenNotFound() {
        // given
        UUID binaryContentId = UUID.randomUUID();
        given(binaryContentRepository.findById(binaryContentId)).willReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() -> binaryContentService.find(binaryContentId));

        // then
        assertThat(thrown).isInstanceOf(BinaryContentNotFoundException.class);
    }

    @Test
    @DisplayName("ID 목록으로 파일 조회 시 DTO 리스트를 반환한다")
    void findAllByIdIn_shouldReturnDtos() {
        // given
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        List<UUID> ids = List.of(firstId, secondId);
        BinaryContent first = new BinaryContent("first", 5L, "text/plain");
        BinaryContent second = new BinaryContent("second", 6L, "text/plain");
        BinaryContentDto firstDto = new BinaryContentDto(firstId, "first", 5L, "text/plain", BinaryContentStatus.PROCESSING);
        BinaryContentDto secondDto = new BinaryContentDto(secondId, "second", 6L, "text/plain", BinaryContentStatus.PROCESSING);
        given(binaryContentRepository.findAllById(ids)).willReturn(List.of(first, second));
        given(binaryContentMapper.toDto(first)).willReturn(firstDto);
        given(binaryContentMapper.toDto(second)).willReturn(secondDto);

        // when
        List<BinaryContentDto> result = binaryContentService.findAllByIdIn(ids);

        // then
        assertThat(result).containsExactly(firstDto, secondDto);
    }

    @Test
    @DisplayName("파일이 존재하면 삭제한다")
    void delete_shouldRemoveBinaryContentWhenExists() {
        // given
        UUID binaryContentId = UUID.randomUUID();
        given(binaryContentRepository.existsById(binaryContentId)).willReturn(true);

        // when
        binaryContentService.delete(binaryContentId);

        // then
        then(binaryContentRepository).should().deleteById(binaryContentId);
    }

    @Test
    @DisplayName("파일이 존재하지 않으면 삭제 시 예외를 던진다")
    void delete_shouldThrowExceptionWhenMissing() {
        // given
        UUID binaryContentId = UUID.randomUUID();
        given(binaryContentRepository.existsById(binaryContentId)).willReturn(false);

        // when
        Throwable thrown = catchThrowable(() -> binaryContentService.delete(binaryContentId));

        // then
        assertThat(thrown).isInstanceOf(BinaryContentNotFoundException.class);
        then(binaryContentRepository).should(never()).deleteById(any());
    }

    @Test
    @DisplayName("상태 변경 시 저장 및 SSE 브로드캐스트를 수행한다")
    void updateStatus_shouldPersistAndBroadcast() {
        // given
        UUID binaryContentId = UUID.randomUUID();
        BinaryContent binaryContent = new BinaryContent("file.txt", 4L, "text/plain");
        BinaryContentDto dto = new BinaryContentDto(
            binaryContentId, "file.txt", 4L, "text/plain", BinaryContentStatus.SUCCESS
        );
        given(binaryContentRepository.findById(binaryContentId)).willReturn(Optional.of(binaryContent));
        given(binaryContentMapper.toDto(binaryContent)).willReturn(dto);

        // when
        BinaryContentDto result = binaryContentService.updateStatus(binaryContentId, BinaryContentStatus.SUCCESS);

        // then
        assertThat(result).isEqualTo(dto);
        then(binaryContentRepository).should().save(binaryContent);
        then(sseService).should().broadcast("binaryContents.updated", dto);
    }

    @Test
    @DisplayName("상태 변경 대상이 없으면 예외를 던진다")
    void updateStatus_shouldThrowWhenBinaryContentMissing() {
        // given
        UUID binaryContentId = UUID.randomUUID();
        given(binaryContentRepository.findById(binaryContentId)).willReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() -> binaryContentService.updateStatus(binaryContentId, BinaryContentStatus.FAIL));

        // then
        assertThat(thrown).isInstanceOf(BinaryContentNotFoundException.class);
        then(binaryContentRepository).should(never()).save(any(BinaryContent.class));
        then(sseService).shouldHaveNoInteractions();
    }
}
