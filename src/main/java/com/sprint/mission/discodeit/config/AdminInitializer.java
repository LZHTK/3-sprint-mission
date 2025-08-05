package com.sprint.mission.discodeit.config;

import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.UserRepository;
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
        // ADMIN 권한을 가진 사용자가 있는지 확인
        boolean adminExists = userRepository.existsByRole(Role.ADMIN);

        if (!adminExists) {
            log.info("ADMIN 계정이 존재하지 않습니다. 초기 ADMIN 계정을 생성합니다.");

            User adminUser = new User(
                "admin",
                "admin@discodeit.com",
                passwordEncoder.encode("admin123!"),
                null
            );
            adminUser.updateRole(Role.ADMIN);

            userRepository.save(adminUser);
            log.info("초기 ADMIN 계정이 생성되었습니다. (username : admin)");
        }
    }
}
