package com.sprint.mission.discodeit.config;

import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== AdminInitializer 실행 시작 ===");

        boolean adminExists = userRepository.existsByRole(Role.ADMIN);
        log.info("ADMIN 사용자 존재 여부: {}", adminExists);

        if (!adminExists) {
            log.info("ADMIN 계정이 존재하지 않습니다. 초기 ADMIN 계정을 생성합니다.");

            // 1. User 먼저 생성
            User adminUser = new User(
                "admin",
                "admin@discodeit.com",
                passwordEncoder.encode("admin123!"),
                null
            );
            adminUser.updateRole(Role.ADMIN);

            // 2. User를 먼저 저장 (ID 생성을 위해)
            User savedUser = userRepository.save(adminUser);
            log.info("Admin 사용자 저장 완료: ID = {}", savedUser.getId());

            // 3. UserStatus 생성 및 양방향 관계 설정
            UserStatus userStatus = new UserStatus(savedUser, Instant.now());
            savedUser.setStatus(userStatus);

            // 4. 다시 저장하여 UserStatus 영속화
            userRepository.save(savedUser);

            log.info("초기 ADMIN 계정과 UserStatus 생성 완료 (username: admin)");

            // 5. 검증
            User verifyUser = userRepository.findByUsername("admin").orElse(null);
            if (verifyUser != null && verifyUser.getStatus() != null) {
                log.info("✅ ADMIN 계정 UserStatus 생성 검증 성공");
            } else {
                log.error("❌ ADMIN 계정 UserStatus 생성 검증 실패");
            }
        }
    }
}
