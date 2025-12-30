package com.sprint.mission.discodeit.event.kafka;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.event.MessageCreateEvent;
import com.sprint.mission.discodeit.event.RoleUpdatedEvent;
import com.sprint.mission.discodeit.event.S3UploadFailedEvent;
import com.sprint.mission.discodeit.event.message.UserLogInOutEvent;
import com.sprint.mission.discodeit.entity.Role;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
public class KafkaProduceRequiredEventListenerTest {

    @Mock private KafkaTemplate<String, String> kafkaTemplate;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private KafkaProduceRequiredEventListener listener;

    @Test
    @DisplayName("MessageCreateEvent를 수신하면 JSON 직렬화 후 메시지를 전송한다")
    void onMessageCreateEvent_success() throws Exception {
        // given
        MessageCreateEvent event = new MessageCreateEvent(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "kim", "dev", "hello");
        given(objectMapper.writeValueAsString(event)).willReturn("{\"message\":true}");

        // when
        listener.on(event);

        // then
        then(kafkaTemplate).should()
            .send("discodeit.MessageCreateEvent", "{\"message\":true}");
    }

    @Test
    @DisplayName("MessageCreateEvent 직렬화 실패 시 예외를 전파하지 않는다")
    void onMessageCreateEvent_failure() throws Exception {
        // given: ObjectMapper가 예외를 던지는 상황
        MessageCreateEvent event = new MessageCreateEvent(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "kim", "dev", "hello");
        given(objectMapper.writeValueAsString(event)).willThrow(new RuntimeException("boom"));

        // when: 이벤트 발행 시도
        ThrowingCallable when = () -> listener.on(event);

        // then: 예외가 외부로 던져지지 않고 KafkaTemplate은 호출되지 않음
        assertThatCode(when).doesNotThrowAnyException();
        then(kafkaTemplate).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("RoleUpdatedEvent를 받아 role 토픽으로 메시지를 전송한다")
    void onRoleUpdatedEvent_success() throws Exception {
        // given
        RoleUpdatedEvent event = new RoleUpdatedEvent(UUID.randomUUID(), Role.USER, Role.ADMIN);
        given(objectMapper.writeValueAsString(event)).willReturn("{\"role\":true}");

        // when
        listener.on(event);

        // then
        then(kafkaTemplate).should()
            .send("discodeit.RoleUpdatedEvent", "{\"role\":true}");
    }

    @Test
    @DisplayName("S3UploadFailedEvent를 받아 S3 토픽으로 메시지를 전송한다")
    void onS3UploadFailedEvent_success() throws Exception {
        // given
        S3UploadFailedEvent event = new S3UploadFailedEvent(
            UUID.randomUUID(), "file.txt", "upload failed", "timeout");
        given(objectMapper.writeValueAsString(event)).willReturn("{\"s3\":true}");

        // when
        listener.on(event);

        // then
        then(kafkaTemplate).should()
            .send("discodeit.S3UploadFailedEvent", "{\"s3\":true}");
    }

    @Test
    @DisplayName("UserLogInOutEvent를 받아 로그인 토픽으로 메시지를 전송한다")
    void onUserLogInOutEvent_success() throws Exception {
        // given
        UserLogInOutEvent event = new UserLogInOutEvent(UUID.randomUUID(), true);
        given(objectMapper.writeValueAsString(event)).willReturn("{\"login\":true}");

        // when
        listener.on(event);

        // then
        then(kafkaTemplate).should()
            .send("discodeit.UserLogInOutEvent", "{\"login\":true}");
    }
}
