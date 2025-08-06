package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.security.DiscodeitUserDetails;
import com.sprint.mission.discodeit.service.AuthService;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class BasicAuthService implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SessionRegistry sessionRegistry;

    @Override
    @Transactional
    public UserDto updateUserRole(UUID userId, Role newRole) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        user.updateRole(newRole);
        User savedUser = userRepository.save(user);

        // 권한이 변경된 사용자의 모든 활성 세션 무효화
        invalidateSessions(user.getUsername());

        log.info("사용자 권한 업데이트 완료 및 세션 무효화 : userId = {} , newRole = {} ", userId, newRole);

        return userMapper.toDto(savedUser);
    }

    /**
     * 특정 사용자의 모든 활성 세션을 무효화
     * */
    private void invalidateSessions(String username) {
        sessionRegistry.getAllSessions(
            // username으로 Princiapl 조회 ( DiscodeitUserDetails 객체 )
            sessionRegistry.getAllPrincipals().stream()
                .filter(principal -> principal instanceof DiscodeitUserDetails)
                .map(principal -> (DiscodeitUserDetails) principal)
                .filter(userDetails -> userDetails.getUsername().equals(username))
                .findFirst()
                .orElse(null),
            false
        ).forEach(sessionInfo -> {
            log.debug("세션 무효화 : sessionId = {}, username = {}", sessionInfo.getSessionId(), username);
            sessionInfo.expireNow();
        });
    }
}
