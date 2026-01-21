package com.sprint.mission.discodeit.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("ChannelMapper 단위 테스트")
public class ChannelMapperTest {

    @Test
    @DisplayName("PRIVATE 채널은 참여자와 마지막 메시지 시간을 매핑한다")
    void privateChannel_mapsParticipantsAndLastMessageAt() {
        // given
        MessageRepository messageRepository = mock(MessageRepository.class);
        ReadStatusRepository readStatusRepository = mock(ReadStatusRepository.class);
        UserMapper userMapper = mock(UserMapper.class);

        ChannelMapperImpl mapper = new ChannelMapperImpl();
        ReflectionTestUtils.setField(mapper, "messageRepository", messageRepository);
        ReflectionTestUtils.setField(mapper, "readStatusRepository", readStatusRepository);
        ReflectionTestUtils.setField(mapper, "userMapper", userMapper);

        Channel channel = new Channel(ChannelType.PRIVATE, "dm", null);
        UUID channelId = UUID.randomUUID();
        ReflectionTestUtils.setField(channel, "id", channelId);

        User user = new User("u1", "u1@test.com", "pw", null);
        UUID userId = UUID.randomUUID();
        ReflectionTestUtils.setField(user, "id", userId);

        when(messageRepository.findLastMessageAtByChannelId(channelId))
            .thenReturn(Optional.of(Instant.parse("2025-01-01T00:00:00Z")));

        ReadStatus rs = new ReadStatus(user, channel, Instant.now());
        when(readStatusRepository.findAllByChannelIdWithUser(channelId))
            .thenReturn(List.of(rs));

        UserDto userDto = new UserDto(userId, "u1", "u1@test.com", Role.USER, null, false);
        when(userMapper.toDto(user)).thenReturn(userDto);

        // when
        var dto = mapper.toDto(channel);

        // then
        assertThat(dto.participants()).hasSize(1);
        assertThat(dto.lastMessageAt()).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"));
    }

    @Test
    @DisplayName("PUBLIC 채널은 참여자 없이 MIN 시간으로 매핑한다")
    void publicChannel_mapsEmptyParticipants() {
        // given
        MessageRepository messageRepository = mock(MessageRepository.class);
        ReadStatusRepository readStatusRepository = mock(ReadStatusRepository.class);
        UserMapper userMapper = mock(UserMapper.class);

        ChannelMapperImpl mapper = new ChannelMapperImpl();
        ReflectionTestUtils.setField(mapper, "messageRepository", messageRepository);
        ReflectionTestUtils.setField(mapper, "readStatusRepository", readStatusRepository);
        ReflectionTestUtils.setField(mapper, "userMapper", userMapper);

        Channel channel = new Channel(ChannelType.PUBLIC, "general", null);
        UUID channelId = UUID.randomUUID();
        ReflectionTestUtils.setField(channel, "id", channelId);

        when(messageRepository.findLastMessageAtByChannelId(channelId))
            .thenReturn(Optional.empty());

        // when
        var dto = mapper.toDto(channel);

        // then
        assertThat(dto.participants()).isEmpty();
        assertThat(dto.lastMessageAt()).isEqualTo(Instant.MIN);
    }
}


