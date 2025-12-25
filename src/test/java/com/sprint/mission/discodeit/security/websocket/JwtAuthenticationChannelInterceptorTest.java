package com.sprint.mission.discodeit.security.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.mission.discodeit.security.jwt.JwtRegistry;
import com.sprint.mission.discodeit.security.jwt.JwtTokenProvider;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationChannelInterceptorTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private JwtRegistry jwtRegistry;
    @Mock private MessageChannel channel;

    @InjectMocks private JwtAuthenticationChannelInterceptor interceptor;

    @Test
    @DisplayName("CONNECT 프레임에 유효한 토큰이 있으면 accessor.setUser가 호출된다")
    void preSend_withValidToken_setsUser() {
        // given
        Message<byte[]> message = buildConnectMessage("Bearer access.jwt");
        given(jwtTokenProvider.validateToken("access.jwt")).willReturn(true);
        given(jwtRegistry.hasActiveJwtInformationByAccessToken("access.jwt")).willReturn(true);
        given(jwtTokenProvider.getTokenType("access.jwt")).willReturn("access");
        given(jwtTokenProvider.extractUsername("access.jwt")).willReturn("kim");
        given(jwtTokenProvider.extractUserId("access.jwt")).willReturn(UUID.randomUUID());
        given(jwtTokenProvider.extractEmail("access.jwt")).willReturn("kim@sprint.io");
        given(jwtTokenProvider.extractRoles("access.jwt")).willReturn(List.of("Role_USER"));

        // when
        Message<?> result = interceptor.preSend(message, channel);

        // then
        assertThat(result).isSameAs(message);
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor.getUser()).isNotNull();
    }

    @Test
    @DisplayName("CONNECT 프레임에 토큰이 없으면 토큰 검증 로직이 호출되지 않는다")
    void preSend_withoutToken() {
        // given
        Message<byte[]> message = buildConnectMessage(null);

        // when
        assertThatThrownBy(() -> interceptor.preSend(message, channel))
            .isInstanceOf(IllegalArgumentException.class);

        // then
        then(jwtTokenProvider).shouldHaveNoInteractions();
        then(jwtRegistry).shouldHaveNoInteractions();
    }

    private Message<byte[]> buildConnectMessage(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.COMMIT);
        accessor.setLeaveMutable(true); // <- 추가
        if (authorization != null) {
            accessor.addNativeHeader("Authorization", authorization);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}

