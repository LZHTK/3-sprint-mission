package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.dto.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelUpdateRequest;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.channel.ChannelNameAlreadyExistsException;
import com.sprint.mission.discodeit.exception.channel.ChannelNotFoundException;
import com.sprint.mission.discodeit.exception.channel.PrivateChannelUpdateNotAllowedException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.ChannelMapper;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.ChannelService;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class BasicChannelService implements ChannelService {

  private final ChannelRepository channelRepository;
  private final ReadStatusRepository readStatusRepository;
  private final MessageRepository messageRepository;
  private final UserRepository userRepository;
  private final ChannelMapper channelMapper;

  @Transactional
  @Override
  public ChannelDto create(PublicChannelCreateRequest request) {
    String name = request.name();
    String description = request.description();
    if (channelRepository.existsChannelByName(name)) {
      log.error("[Public 채널 생성 실패] 해당 채널 이름은 이미 존재합니다. 채널 이름 : {}", name);
      throw new ChannelNameAlreadyExistsException();
    }

    Channel channel = new Channel(ChannelType.PUBLIC, name, description);
    log.info("[Public 채널 생성 시도] 채널 이름 : {}", name);

    // ID가 할당되어있는 저장된 엔티티를 받아서 사용
    Channel savedChannel = channelRepository.save(channel);
    log.info("[Public 채널 생성 성공] 채널 이름 : {} ", name);
    return channelMapper.toDto(savedChannel);

  }

  @Transactional
  @Override
  public ChannelDto create(PrivateChannelCreateRequest request) {
    List<UUID> participantIds = request.participantIds();

    List<User> foundUsers = userRepository.findAllById(participantIds);
    if (foundUsers.size() != participantIds.size()) {
      log.error("[Private 채널 생성 실패] 존재하지 않는 유저 ID가 포함되어 있습니다. 요청된 유저 수: {}, 찾은 유저 수: {}",
          participantIds.size(), foundUsers.size());
      throw new UserNotFoundException();
    }

    Channel channel = new Channel(ChannelType.PRIVATE, null, null);
    log.info("[Private 채널 생성 시도] 채널 ID : {}", channel.getId());

    // ID가 할당되어있는 저장된 엔티티를 받아서 사용
    Channel savedChannel = channelRepository.save(channel);
    log.info("[Private 채널 생성 성공] 채널 ID : {} ", channel.getId());

    List<ReadStatus> readStatuses = foundUsers.stream()
        .map(user -> new ReadStatus(user, channel, channel.getCreatedAt()))
        .toList();
    log.info("[읽음 상태 생성 시도] 읽음 상태 갯수 : {} ", readStatuses.size());

    readStatusRepository.saveAll(readStatuses);
    log.info("[읽음 상태 생성 성공] 읽음 상태 갯수 : {} ", readStatuses.size());

    return channelMapper.toDto(savedChannel);

  }

  @Transactional(readOnly = true)
  @Override
  public ChannelDto find(UUID channelId) {
    log.info("[채널 조회 시도] 읽음 상태 ID : {} ", channelId);

    return channelRepository.findById(channelId)
        .map(channelMapper::toDto)
        .orElseThrow(() -> {
          log.error("[채널 조회 실패] 채널 ID : {}", channelId);
          return new PrivateChannelUpdateNotAllowedException();
        });
  }

  @Transactional(readOnly = true)
  @Override
  public List<ChannelDto> findAllByUserId(UUID userId) {
    log.info("[유저가 참여한 모든 채널 조회 시도] 유저 ID : {} ", userId);

    List<UUID> mySubscribedChannelIds = readStatusRepository.findAllByUserId(userId).stream()
        .map(ReadStatus::getChannel)
        .map(Channel::getId)
        .toList();
    log.info("[유저가 참여한 모든 채널 조회 성공] 채널 IDs : {} ", mySubscribedChannelIds);

    return channelRepository.findAllByTypeOrIdIn(ChannelType.PUBLIC, mySubscribedChannelIds)
        .stream()
        .map(channelMapper::toDto)
        .toList();
  }

  @Transactional
  @Override
  public ChannelDto update(UUID channelId, PublicChannelUpdateRequest request) {
    String newName = request.newName();
    String newDescription = request.newDescription();
    log.info("[채널 수정 시도] 채널 ID : {} ", channelId);

    Channel channel = channelRepository.findById(channelId)
        .orElseThrow(() -> {
          log.error("[채널 수정 실패] 해당 채널이 존재하지 않습니다. 채널 ID : {} ", channelId);
          return new ChannelNotFoundException();
        });

    if (channel.getType().equals(ChannelType.PRIVATE)) {
      log.error("[채널 수정 실패] 개인 채널은 수정할 수 없습니다. 채널 ID : {} ", channelId);
      throw new PrivateChannelUpdateNotAllowedException();
    }

    channel.update(newName, newDescription);
    log.info("[채널 수정 성공] 채널 ID : {} ", channelId);

    return channelMapper.toDto(channel);
  }

  @Transactional
  @Override
  public void delete(UUID channelId) {
    log.info("[채널 삭제 시도] 채널 ID : {} ", channelId);

    if (!channelRepository.existsById(channelId)) {
      log.error("[채널 삭제 실패] 해당 채널을 찾을 수 없습니다. 채널 ID : {} ", channelId);
      throw new ChannelNotFoundException();
    }

    messageRepository.deleteAllByChannelId(channelId);
    readStatusRepository.deleteAllByChannelId(channelId);
    log.info("[채널 삭제 성공] 채널 ID : {} ", channelId);

    channelRepository.deleteById(channelId);
  }
}
