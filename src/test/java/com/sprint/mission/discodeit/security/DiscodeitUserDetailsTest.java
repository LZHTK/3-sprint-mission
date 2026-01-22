package com.sprint.mission.discodeit.security;
import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import java.util.Collection;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

public class DiscodeitUserDetailsTest {

    @Test
    @DisplayName("getAuthorities는 역할 권한을 반환한다")
    void getAuthorities_returnsRoleAuthority() {
        // given
        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, "user", "email", Role.USER, null, false);
        User user = new User("user", "email", "pwd", null);
        DiscodeitUserDetails details = new DiscodeitUserDetails(userDto, "pwd", user);

        // when
        Collection<? extends GrantedAuthority> authorities = details.getAuthorities();

        // then
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority())
            .isEqualTo(Role.USER.getAuthority());
    }

    @Test
    @DisplayName("getUsername과 getUserId는 사용자 정보를 반환한다")
    void getters_returnUserInfo() {
        // given
        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, "user", "email", Role.USER, null, false);
        User user = new User("user", "email", "pwd", null);
        DiscodeitUserDetails details = new DiscodeitUserDetails(userDto, "pwd", user);

        // when
        String username = details.getUsername();
        UUID id = details.getUserId();

        // then
        assertThat(username).isEqualTo("user");
        assertThat(id).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("equals/hashCode는 username 기준으로 동등성을 판단한다")
    void equalsHashCode_byUsername() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        UserDto dto1 = new UserDto(id1, "same", "a@a.com", Role.USER, null, false);
        UserDto dto2 = new UserDto(id2, "same", "b@b.com", Role.USER, null, false);

        User user1 = new User("same", "a@a.com", "pwd", null);
        User user2 = new User("same", "b@b.com", "pwd", null);

        DiscodeitUserDetails d1 = new DiscodeitUserDetails(dto1, "pwd", user1);
        DiscodeitUserDetails d2 = new DiscodeitUserDetails(dto2, "pwd", user2);

        // when
        boolean eq = d1.equals(d2);
        int h1 = d1.hashCode();
        int h2 = d2.hashCode();

        // then
        assertThat(eq).isTrue();
        assertThat(h1).isEqualTo(h2);
    }

    @Test
    @DisplayName("equals는 null 또는 다른 타입과 비교 시 false")
    void equals_withNullOrOtherType_isFalse() {
        // given
        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, "user", "email", Role.USER, null, false);
        User user = new User("user", "email", "pwd", null);
        DiscodeitUserDetails details = new DiscodeitUserDetails(userDto, "pwd", user);

        // when
        boolean eqNull = details.equals(null);
        boolean eqOther = details.equals("other");

        // then
        assertThat(eqNull).isFalse();
        assertThat(eqOther).isFalse();
    }
}
