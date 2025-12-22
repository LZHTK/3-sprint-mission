package com.sprint.mission.discodeit.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import com.sprint.mission.discodeit.service.BinaryContentService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class BinaryContentEventListenerTest {

    @Mock private BinaryContentStorage binaryContentStorage;
    @Mock private BinaryContentService binaryContentService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private BinaryContentEventListener listener;

    @Test
    @DisplayName("파일 업로드가 성공하면 상태를 SUCCESS로 변경한다")
    void handleBinaryContentCreated_성공() {
        // given: 정상적인 업로드 이벤트
        UUID id = UUID.randomUUID();
        BinaryContentCreatedEvent event =
            new BinaryContentCreatedEvent(id, "bytes".getBytes(), "file.txt");

        // when: 핸들러 실행
        listener.handleBinaryContentCreated(event);

        // then: 저장소 put 및 상태 업데이트, 실패 이벤트 없음
        then(binaryContentStorage).should().put(id, event.bytes());
        then(binaryContentService).should().updateStatus(id, BinaryContentStatus.SUCCESS);
        then(eventPublisher).should(never()).publishEvent(any(S3UploadFailedEvent.class));
    }

    @Test
    @DisplayName("업로드 실패 시 상태를 FAIL로 변경하고 실패 이벤트를 발행한다")
    void handleBinaryContentCreated_실패() {
        // given: 저장소 put에서 예외 발생
        UUID id = UUID.randomUUID();
        BinaryContentCreatedEvent event =
            new BinaryContentCreatedEvent(id, "bytes".getBytes(), "file.txt");
        doThrow(new RuntimeException("s3 down")).when(binaryContentStorage).put(eq(id), any());

        // when: 핸들러 실행
        listener.handleBinaryContentCreated(event);

        // then: 상태를 FAIL로 바꾸고 S3UploadFailedEvent 발행
        then(binaryContentService).should().updateStatus(id, BinaryContentStatus.FAIL);
        then(eventPublisher).should().publishEvent(any(S3UploadFailedEvent.class));
    }
}

