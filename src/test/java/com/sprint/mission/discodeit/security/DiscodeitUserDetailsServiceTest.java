package com.sprint.mission.discodeit.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscodeitUserDetailsService 단위 테스트")
public class DiscodeitUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private DiscodeitUserDetailsService userDetailsService;

    @Test
    @DisplayName("사용자 이름으로 UserDetails를 조회한다")
    void loadUserByUsername_shouldReturnUserDetails() {
        // given
        UUID userId = UUID.randomUUID();
        User entity = new User("tester", "tester@email.com", "pw", null);
        ReflectionTestUtils.setField(entity, "id", userId);
        UserDto dto = new UserDto(userId, "tester", "tester@email.com", Role.USER, null, true);
        given(userRepository.findByUsername("tester")).willReturn(Optional.of(entity));
        given(userMapper.toDto(entity)).willReturn(dto);

        // when
        var result = userDetailsService.loadUserByUsername("tester");

        // then
        assertThat(result.getUsername()).isEqualTo("tester");
        assertThat(result.getPassword()).isEqualTo("pw");
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("사용자를 찾지 못하면 UsernameNotFoundException을 던진다")
    void loadUserByUsername_shouldThrowWhenUserMissing() {
        // given
        given(userRepository.findByUsername("ghost")).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost"))
            .isInstanceOf(UsernameNotFoundException.class);
    }
}
