package com.sprint.mission.discodeit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserUpdateRequest;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.event.BinaryContentCreatedEvent;
import com.sprint.mission.discodeit.event.RoleUpdatedEvent;
import com.sprint.mission.discodeit.exception.user.UserEmailAlreadyExistsException;
import com.sprint.mission.discodeit.exception.user.UserNameAlreadyExistsException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.basic.BasicUserService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
public class BasicUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private BinaryContentRepository binaryContentRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserSessionService userSessionService;
    @Mock private CacheManager cacheManager;
    @Mock private SseService sseService;

    @InjectMocks
    private BasicUserService userService;

    @Test
    @DisplayName("User 생성 -  case : success")
    void createUserSuccess() {
        // Given
        UserCreateRequest userCreateRequest = new UserCreateRequest("KHG", "KHG@test.com", "009874");
        User user = new User(userCreateRequest.username(), userCreateRequest.email(), userCreateRequest.password(), null);
        UserDto userDto = new UserDto(user.getId(), user.getUsername(), user.getEmail(), Role.USER
            , null,true);
        given(userRepository.existsByUsername(userCreateRequest.username())).willReturn(false);
        given(userRepository.existsByEmail(userCreateRequest.email())).willReturn(false);
        given(userRepository.save(any(User.class))).willReturn(user);
        given(passwordEncoder.encode(any())).willReturn("encoded");
        given(userMapper.toDto(any(User.class), eq(false))).willReturn(userDto);

        // When
        UserDto result = userService.create(userCreateRequest, Optional.empty());

        // Then
        assertThat(result.username()).isEqualTo("KHG");
    }

    @Test
    @DisplayName("User 생성 - case : 중복된 이름으로 인한 failed")
    void createUserFail() {
        // Given
        UserCreateRequest userCreateRequest = new UserCreateRequest("KHG", "KHG@test.com",
            "009874");
        User user = new User(userCreateRequest.username(), userCreateRequest.email(),
            userCreateRequest.password(), null);
        given(userRepository.existsByUsername(user.getUsername())).willReturn(true);

        // When
        ThrowingCallable when = () -> userService.create(userCreateRequest, Optional.empty());

        // Then
        assertThatThrownBy(when)
            .isInstanceOf(UserNameAlreadyExistsException.class)
            .hasMessageContaining("중복된 유저 이름입니다.");
    }

    @Test
    @DisplayName("User 조회 - case : success")
    void findUserSuccess() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = new User("KHG", "KHG@test.com", "009874", null);
        UserDto expectedDto = new UserDto(userId, "KHG", "KHG@test.com", Role.USER
            , null, false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userMapper.toDto(user, false)).willReturn(expectedDto);
        given(userSessionService.isUserOnline(any())).willReturn(false);

        // When
        UserDto result = userService.find(userId);

        // Then
        assertThat(result.username()).isEqualTo("KHG");
        assertThat(result.email()).isEqualTo("KHG@test.com");
    }

    @Test
    @DisplayName("User 조회 - case : 존재하지 않는 유저로 인한 failed")
    void findUserFail() {
        // Given
        UUID userId = UUID.randomUUID();
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When
        ThrowingCallable when = () -> userService.find(userId);

        // Then
        assertThatThrownBy(when)
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining("유저를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("User 수정 - case : success")
    void updatedUserSuccess() {
        // Given
        UUID userId = UUID.randomUUID();
        User existingUser = new User("test", "test@test.com","9874",null);
        UserUpdateRequest userUpdateRequest = new UserUpdateRequest("KHG","KHG@test.com","009874");
        UserDto expectedDto = new UserDto(userId, userUpdateRequest.newUsername(), userUpdateRequest.newEmail(), Role.USER
            , null, true);
        given(userRepository.findById(userId)).willReturn(Optional.of(existingUser));
        given(userRepository.existsByUsername(userUpdateRequest.newUsername())).willReturn(false);
        given(userMapper.toDto(eq(existingUser), anyBoolean())).willReturn(expectedDto);

        // When
        UserDto result = userService.update(userId, userUpdateRequest, Optional.empty());

        // Then
        assertThat(result).isEqualTo(expectedDto);
    }

    @Test
    @DisplayName("User 수정 - case : 중복된 이메일로 인한 failed")
    void updateUserFail() {
        // Given
        UUID userID = UUID.randomUUID();
        User existngUser = new User("test", "test@test.com", "9874", null);
        UserUpdateRequest userUpdateRequest = new UserUpdateRequest("KHG","test@test.com","009874");
        given(userRepository.findById(userID)).willReturn(Optional.of(existngUser));
        given(userRepository.existsByEmail(userUpdateRequest.newEmail())).willReturn(true);

        // When
        ThrowingCallable when = () -> userService.update(userID, userUpdateRequest, Optional.empty());

        // Then
        assertThatThrownBy(when)
            .isInstanceOf(UserEmailAlreadyExistsException.class)
            .hasMessageContaining("중복된 이메일입니다.");
    }

    @Test
    @DisplayName("프로필 저장 중 오류가 발생하면 사용자 저장을 중단한다")
    void updateUserProfile_저장실패() {
        // given
        UUID userId = UUID.randomUUID();
        User user = new User("kim", "kim@sprint.io", "pwd", null);
        BinaryContentCreateRequest profileRequest =
            new BinaryContentCreateRequest("profile.png", "image/png", "img".getBytes());

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        willThrow(new RuntimeException("binary content error"))
            .given(binaryContentRepository)
            .save(any(BinaryContent.class));

        // when
        ThrowingCallable when = () ->
            userService.update(userId,
                new UserUpdateRequest("newName", "newEmail", "newPwd"),
                Optional.of(profileRequest));

        // then
        assertThatThrownBy(when).isInstanceOf(RuntimeException.class);
        then(binaryContentRepository).should().save(any(BinaryContent.class));
        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    @DisplayName("User 삭제 - case : success")
    void deleteUserSuccess() {
        // given
        UUID userId = UUID.randomUUID();
        User user = new User("test", "test@test.com", "009874", null);
        UserDto dto = new UserDto(userId, "test", "test@test.com", Role.USER, null, false);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userSessionService.isUserOnline(userId)).willReturn(false);
        given(userMapper.toDto(user, false)).willReturn(dto);
        given(userRepository.existsById(userId)).willReturn(true);

        willDoNothing().given(userSessionService).markUserOffline(userId);
        willDoNothing().given(sseService).broadcast(eq("users.deleted"), any());
        willDoNothing().given(userRepository).deleteById(userId);

        // when
        userService.delete(userId);

        // then
        then(userRepository).should().findById(userId);
        then(userSessionService).should().markUserOffline(userId);
        then(userRepository).should().deleteById(userId);
    }

    @Test
    @DisplayName("User 삭제 - case : 존재하지 않는 유저로 인한 failed")
    void deleteUserFail() {
        // given
        UUID userId = UUID.randomUUID();
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() -> userService.delete(userId));

        // then
        assertThat(thrown)
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining("유저를 찾을 수 없습니다.");
        then(userRepository).should(never()).deleteById(userId);
    }

    @Test
    @DisplayName("프로필 요청이 있는 생성은 바이너리 저장 · 이벤트 발행 · SSE 브로드캐스트를 수행한다")
    void createUser_withProfile_shouldPersistBinaryAndPublishEvent() {
        // given
        Cache usersCache = mock(Cache.class);
        given(cacheManager.getCache("users")).willReturn(usersCache);

        UserCreateRequest request = new UserCreateRequest("alpha", "alpha@sprint.io", "plain");
        BinaryContentCreateRequest profile = new BinaryContentCreateRequest(
            "profile.png", "image/png", "img".getBytes());

        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(userRepository.existsByUsername(request.username())).willReturn(false);
        given(passwordEncoder.encode("plain")).willReturn("encoded");

        User persisted = new User(request.username(), request.email(), "encoded", null);
        given(userRepository.save(any(User.class))).willReturn(persisted);

        UserDto dto = new UserDto(UUID.randomUUID(), request.username(), request.email(), Role.USER, null, false);
        given(userMapper.toDto(any(User.class), eq(false))).willReturn(dto);

        // when
        UserDto result = userService.create(request, Optional.of(profile));

        // then
        assertThat(result).isEqualTo(dto);
        then(usersCache).should().clear();
        then(binaryContentRepository).should().save(any(BinaryContent.class));
        then(eventPublisher).should().publishEvent(any(BinaryContentCreatedEvent.class));
        then(sseService).should().broadcast("users.created", dto);
    }

    @Test
    @DisplayName("SSE 브로드캐스트 실패는 사용자 생성 결과에 영향을 주지 않는다")
    void createUser_shouldHandleSseFailureGracefully() {
        // given
        UserCreateRequest request = new UserCreateRequest("beta", "beta@sprint.io", "plain");
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(userRepository.existsByUsername(request.username())).willReturn(false);
        given(passwordEncoder.encode("plain")).willReturn("encoded");

        User persisted = new User(request.username(), request.email(), "encoded", null);
        given(userRepository.save(any(User.class))).willReturn(persisted);

        UserDto dto = new UserDto(UUID.randomUUID(), request.username(), request.email(), Role.USER, null, false);
        given(userMapper.toDto(any(User.class), eq(false))).willReturn(dto);
        willThrow(new RuntimeException("sse down"))
            .given(sseService).broadcast(anyString(), any());

        // when
        UserDto result = userService.create(request, Optional.empty());

        // then
        assertThat(result).isEqualTo(dto);
    }

    @Test
    @DisplayName("전체 사용자 조회는 각 사용자 온라인 여부를 반영해 DTO로 변환한다")
    void findAll_shouldConsultSessionService() {
        // given
        User user1 = new User("kim", "kim@sprint.io", "pwd", null);
        User user2 = new User("lee", "lee@sprint.io", "pwd", null);
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        ReflectionTestUtils.setField(user1, "id", user1Id);
        ReflectionTestUtils.setField(user2, "id", user2Id);

        given(userRepository.findAllWithProfileAndStatus()).willReturn(List.of(user1, user2));
        given(userSessionService.isUserOnline(user1Id)).willReturn(true);
        given(userSessionService.isUserOnline(user2Id)).willReturn(false);

        UserDto dto1 = new UserDto(user1Id, "kim", "kim@sprint.io", Role.USER, null, true);
        UserDto dto2 = new UserDto(user2Id, "lee", "lee@sprint.io", Role.USER, null, false);
        given(userMapper.toDto(user1, true)).willReturn(dto1);
        given(userMapper.toDto(user2, false)).willReturn(dto2);

        // when
        List<UserDto> result = userService.findAll();

        // then
        assertThat(result).containsExactly(dto1, dto2);
    }

    @Test
    @DisplayName("역할이 변경되면 RoleUpdatedEvent와 SSE 브로드캐스트를 발행한다")
    void updateRole_shouldPublishEventAndBroadcast() {
        // given
        UUID userId = UUID.randomUUID();
        User user = new User("kim", "kim@sprint.io", "pwd", null);
        ReflectionTestUtils.setField(user, "id", userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userSessionService.isUserOnline(userId)).willReturn(false);

        UserDto dto = new UserDto(userId, "kim", "kim@sprint.io", Role.ADMIN, null, false);
        given(userMapper.toDto(user, false)).willReturn(dto);

        // when
        UserDto result = userService.updateRole(userId, Role.ADMIN);

        // then
        assertThat(result).isEqualTo(dto);
        ArgumentCaptor<RoleUpdatedEvent> captor = ArgumentCaptor.forClass(RoleUpdatedEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        assertThat(captor.getValue().oldRole()).isEqualTo(Role.USER);
        assertThat(captor.getValue().newRole()).isEqualTo(Role.ADMIN);
        then(sseService).should().broadcast("users.updated", dto);
    }

    @Test
    @DisplayName("역할이 그대로면 이벤트 발행 없이 SSE만 보낸다")
    void updateRole_shouldSkipEventWhenRoleUnchanged() {
        // given
        UUID userId = UUID.randomUUID();
        User user = new User("kim", "kim@sprint.io", "pwd", null);
        ReflectionTestUtils.setField(user, "id", userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userSessionService.isUserOnline(userId)).willReturn(true);

        UserDto dto = new UserDto(userId, "kim", "kim@sprint.io", Role.USER, null, true);
        given(userMapper.toDto(user, true)).willReturn(dto);

        // when
        userService.updateRole(userId, Role.USER);

        // then
        then(eventPublisher).shouldHaveNoInteractions();
        then(sseService).should().broadcast("users.updated", dto);
    }

    @Test
    @DisplayName("refreshUserListCache는 DB에서 최신 사용자 목록을 읽어온다")
    void refreshUserListCache_shouldReloadUsers() {
        // given
        User user = new User("kim", "kim@sprint.io", "pwd", null);
        given(userRepository.findAll()).willReturn(List.of(user));

        UserDto dto = new UserDto(UUID.randomUUID(), "kim", "kim@sprint.io", Role.USER, null, null);
        given(userMapper.toDto(user)).willReturn(dto);

        // when
        List<UserDto> result = userService.refreshUserListCache();

        // then
        assertThat(result).containsExactly(dto);
        then(userRepository).should().findAll();
    }

    @Test
    @DisplayName("사용자 관련 캐시는 명시적으로 제거할 수 있다")
    void clearUserRelatedCaches_shouldEvictChannelAndNotificationEntries() {
        // given
        Cache channelCache = mock(Cache.class);
        Cache notificationCache = mock(Cache.class);
        given(cacheManager.getCache("userChannels")).willReturn(channelCache);
        given(cacheManager.getCache("userNotifications")).willReturn(notificationCache);

        // when
        UUID userId = UUID.randomUUID();
        userService.clearUserRelatedCaches(userId);

        // then
        then(channelCache).should().evict(userId);
        then(notificationCache).should().evict(userId);
    }
}



