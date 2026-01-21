package com.sprint.mission.discodeit.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.discodeit.dto.data.MessageDto;
import com.sprint.mission.discodeit.dto.data.ReadStatusDto;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Mapper 핵심 필드 매핑 테스트")
public class MapperKeyFieldsTest {

    @Test
    @DisplayName("ReadStatusMapper는 userId와 channelId를 매핑한다")
    void readStatusMapper_mapsIds() {
        // given
        ReadStatusMapperImpl mapper = new ReadStatusMapperImpl();

        User user = new User("u1", "u1@test.com", "pw", null);
        UUID userId = UUID.randomUUID();
        ReflectionTestUtils.setField(user, "id", userId);

        Channel channel = new Channel(ChannelType.PUBLIC, "c", null);
        UUID channelId = UUID.randomUUID();
        ReflectionTestUtils.setField(channel, "id", channelId);

        ReadStatus readStatus = new ReadStatus(user, channel, Instant.now());

        // when
        ReadStatusDto dto = mapper.toDto(readStatus);

        // then
        assertThat(dto.userId()).isEqualTo(userId);
        assertThat(dto.channelId()).isEqualTo(channelId);
    }

    @Test
    @DisplayName("MessageMapper는 channelId를 매핑한다")
    void messageMapper_mapsChannelId() {
        // given
        BinaryContentMapperImpl binaryContentMapper = new BinaryContentMapperImpl();
        UserMapperImpl userMapper = new UserMapperImpl();
        ReflectionTestUtils.setField(userMapper, "binaryContentMapper", binaryContentMapper);

        MessageMapperImpl messageMapper = new MessageMapperImpl();
        ReflectionTestUtils.setField(messageMapper, "binaryContentMapper", binaryContentMapper);
        ReflectionTestUtils.setField(messageMapper, "userMapper", userMapper);

        Channel channel = new Channel(ChannelType.PUBLIC, "c", null);
        UUID channelId = UUID.randomUUID();
        ReflectionTestUtils.setField(channel, "id", channelId);

        User user = new User("u1", "u1@test.com", "pw", null);
        Message message = new Message("hi", channel, user,
            List.of(new BinaryContent("f", 1L, "text/plain")));

        // when
        MessageDto dto = messageMapper.toDto(message);

        // then
        assertThat(dto.channelId()).isEqualTo(channelId);
    }
}
