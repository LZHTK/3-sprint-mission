package com.sprint.mission.discodeit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.sprint.mission.discodeit.dto.data.ReadStatusDto;
import com.sprint.mission.discodeit.dto.request.ReadStatusCreateRequest;
import com.sprint.mission.discodeit.dto.request.ReadStatusUpdateRequest;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.readstatus.ReadStatusAlreadyExistsException;
import com.sprint.mission.discodeit.exception.readstatus.ReadStatusNotFoundException;
import com.sprint.mission.discodeit.mapper.ReadStatusMapper;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.basic.BasicReadStatusService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BasicReadStatusServiceTest {

    @Mock private ReadStatusRepository readStatusRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChannelRepository channelRepository;
    @Mock private ReadStatusMapper readStatusMapper;

    @InjectMocks private BasicReadStatusService readStatusService;

    @Test
    @DisplayName("읽음 상태 생성 시 사용자·채널 검증 후 저장과 DTO 변환을 수행한다")
    void createReadStatus_success() {
        // given: 유효한 사용자와 채널이 존재하고 중복 데이터가 없다
        UUID userId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        Instant now = Instant.now();
        ReadStatusCreateRequest request = new ReadStatusCreateRequest(userId, channelId, now);
        User user = new User("kim", "kim@sprint.io", "pw", null);
        Channel channel = new Channel(ChannelType.PUBLIC, "general", "desc");
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(channel, "id", channelId);
        ReadStatusDto dto = new ReadStatusDto(UUID.randomUUID(), userId, channelId, now, true);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));
        given(readStatusRepository.existsByUserIdAndChannelId(userId, channelId)).willReturn(false);
        given(readStatusMapper.toDto(any(ReadStatus.class))).willReturn(dto);

        // when: 서비스로 생성 요청
        ReadStatusDto result = readStatusService.create(request);

        // then: 저장과 DTO 반환이 정상 수행됨
        assertThat(result).isEqualTo(dto);
        then(readStatusRepository).should().save(any(ReadStatus.class));
    }

    @Test
    @DisplayName("이미 읽음 상태가 존재하면 예외를 던진다")
    void createReadStatus_duplicate() {
        // given: 동일 userId/channelId 조합이 이미 존재
        UUID userId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        ReadStatusCreateRequest request = new ReadStatusCreateRequest(userId, channelId, Instant.now());
        User user = new User("kim", "kim@sprint.io", "pw", null);
        Channel channel = new Channel(ChannelType.PUBLIC, "general", "desc");
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(channel, "id", channelId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));
        given(readStatusRepository.existsByUserIdAndChannelId(userId, channelId)).willReturn(true);

        // when
        ThrowingCallable when = () -> readStatusService.create(request);

        // then
        assertThatThrownBy(when).isInstanceOf(ReadStatusAlreadyExistsException.class);
        then(readStatusRepository).should(never()).save(any(ReadStatus.class));
    }

    @Test
    @DisplayName("읽음 상태 업데이트 시 시간과 알림 여부가 모두 갱신된다")
    void updateReadStatus_updatesFields() {
        // given: 기존 ReadStatus가 존재하며 새로운 시간/알림 설정을 전달
        UUID readStatusId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        User user = new User("kim", "kim@sprint.io", "pw", null);
        Channel channel = new Channel(ChannelType.PRIVATE, "dm", "desc");
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(channel, "id", channelId);
        ReadStatus readStatus = new ReadStatus(user, channel, Instant.now().minusSeconds(100));
        ReadStatusUpdateRequest request = new ReadStatusUpdateRequest(Instant.now(), false);
        ReadStatusDto dto = new ReadStatusDto(readStatusId, userId, channelId, request.newLastReadAt(), false);
        given(readStatusRepository.findById(readStatusId)).willReturn(Optional.of(readStatus));
        given(readStatusMapper.toDto(readStatus)).willReturn(dto);

        // when: 업데이트 실행
        ReadStatusDto result = readStatusService.update(readStatusId, request);

        // then: 엔티티 상태와 반환 DTO가 모두 기대대로 바뀜
        assertThat(result).isEqualTo(dto);
        assertThat(readStatus.getLastReadAt()).isEqualTo(request.newLastReadAt());
        assertThat(readStatus.isNotificationEnabled()).isFalse();
    }

    @Test
    @DisplayName("삭제 대상이 없으면 ReadStatusNotFoundException을 던진다")
    void deleteReadStatus_notFound() {
        // given
        UUID readStatusId = UUID.randomUUID();
        given(readStatusRepository.existsById(readStatusId)).willReturn(false);

        // when
        ThrowingCallable when = () -> readStatusService.delete(readStatusId);

        // then
        assertThatThrownBy(when).isInstanceOf(ReadStatusNotFoundException.class);
    }
}
