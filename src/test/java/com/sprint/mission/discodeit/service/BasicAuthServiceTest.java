package com.sprint.mission.discodeit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.security.jwt.JwtRegistry;
import com.sprint.mission.discodeit.service.basic.BasicAuthService;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("BasicAuthService 단위 테스트")
public class BasicAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtRegistry jwtRegistry;

    @InjectMocks
    private BasicAuthService authService;

    @Test
    @DisplayName("권한 변경 시 사용자 저장 및 세션 무효화를 수행한다")
    void updateUserRole_shouldPersistAndInvalidateSessions() {
        // given
        UUID userId = UUID.randomUUID();
        User user = new User("alpha", "alpha@test.com", "pw", null);
        UserDto dto = new UserDto(userId, "alpha", "alpha@test.com", Role.ADMIN, null, false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.save(user)).willReturn(user);
        given(jwtRegistry.hasActiveJwtInformationByUserId(userId)).willReturn(true);
        given(userMapper.toDto(user)).willReturn(dto);

        // when
        UserDto result = authService.updateUserRole(userId, Role.ADMIN);

        // then
        assertThat(result).isEqualTo(dto);
        then(jwtRegistry).should().invalidateJwtInformationByUserId(userId);
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("활성 세션이 없으면 무효화 호출을 생략한다")
    void updateUserRole_shouldSkipInvalidationWhenNoActiveSession() {
        // given
        UUID userId = UUID.randomUUID();
        User user = new User("beta", "beta@test.com", "pw", null);
        UserDto dto = new UserDto(userId, "beta", "beta@test.com", Role.CHANNEL_MANAGER, null, true);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.save(user)).willReturn(user);
        given(jwtRegistry.hasActiveJwtInformationByUserId(userId)).willReturn(false);
        given(userMapper.toDto(user)).willReturn(dto);

        // when
        UserDto result = authService.updateUserRole(userId, Role.CHANNEL_MANAGER);

        // then
        assertThat(result).isEqualTo(dto);
        then(jwtRegistry).should(never()).invalidateJwtInformationByUserId(any());
    }

    @Test
    @DisplayName("사용자를 찾지 못하면 예외를 던진다")
    void updateUserRole_shouldThrowWhenUserMissing() {
        // given
        UUID userId = UUID.randomUUID();
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() -> authService.updateUserRole(userId, Role.ADMIN));

        // then
        assertThat(thrown).isInstanceOf(EntityNotFoundException.class);
    }
}
