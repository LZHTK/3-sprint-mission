package com.sprint.mission.discodeit.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class AsyncConfigTest {

    @Test
    @DisplayName("ContextTaskDecorator는 MDC와 SecurityContext를 복원하고 종료 시 정리한다")
    void contextTaskDecorator_restoresAndClearsContext() {
        // given
        MDC.put("requestId", "req-1");
        MDC.put("userId", "user-1");

        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken("user", "pwd");
        SecurityContextHolder.getContext().setAuthentication(auth);

        AsyncConfig config = new AsyncConfig();
        TaskDecorator decorator = config.taskDecorator();

        // when
        Runnable decorated = decorator.decorate(() -> {
            assertThat(MDC.get("requestId")).isEqualTo("req-1");
            assertThat(MDC.get("userId")).isEqualTo("user-1");
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(auth);
        });
        decorated.run();

        // then
        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("userId")).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
