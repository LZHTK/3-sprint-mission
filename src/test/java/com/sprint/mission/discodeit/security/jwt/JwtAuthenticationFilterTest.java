package com.sprint.mission.discodeit.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sprint.mission.discodeit.security.DiscodeitUserDetails;
import com.sprint.mission.discodeit.security.DiscodeitUserDetailsService;
import com.sprint.mission.discodeit.service.UserSessionService;
import jakarta.servlet.FilterChain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.shaded.com.google.common.net.HttpHeaders;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private JwtRegistry jwtRegistry;
    @Mock private DiscodeitUserDetailsService userDetailsService;
    @Mock private UserSessionService userSessionService;

    @InjectMocks private JwtAuthenticationFilter filter;

    @Test
    @DisplayName("Authorization 헤더가 없으면 토큰 검증 로직이 실행되지 않는다")
    void doFilter_noAuthorizationHeader() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        // when
        filter.doFilterInternal(request, response, chain);

        // then
        verifyNoInteractions(jwtTokenProvider, jwtRegistry, userDetailsService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("유효한 액세스 토큰이면 SecurityContext에 인증 객체가 설정된다")
    void doFilter_validToken_setsAuthentication() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        DiscodeitUserDetails userDetails = mock(DiscodeitUserDetails.class);
        UUID userId = UUID.randomUUID();
        Collection<? extends GrantedAuthority> authorities =
            new ArrayList<>(List.of(new SimpleGrantedAuthority("ROLE_USER")));
        BDDMockito.<Collection<? extends GrantedAuthority>>given(userDetails.getAuthorities())
            .willReturn(authorities);
        given(userDetails.getUsername()).willReturn("kim");
        given(userDetails.getUserId()).willReturn(userId);
        given(userDetailsService.loadUserByUsername("kim")).willReturn(userDetails);

        given(jwtTokenProvider.validateToken("access.jwt")).willReturn(true);
        given(jwtRegistry.hasActiveJwtInformationByAccessToken("access.jwt")).willReturn(true);
        given(jwtTokenProvider.getTokenType("access.jwt")).willReturn("access");
        given(jwtTokenProvider.extractUsername("access.jwt")).willReturn("kim");

        // when
        filter.doFilterInternal(request, response, chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("kim");
        then(userSessionService).should().markUserOnline(userId);

        // cleanup
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("활성화되지 않은 토큰이면 SecurityContext를 설정하지 않는다")
    void doFilter_inactiveToken() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer access.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        given(jwtTokenProvider.validateToken("access.jwt")).willReturn(true);
        given(jwtTokenProvider.getTokenType("access.jwt")).willReturn("access");
        given(jwtRegistry.hasActiveJwtInformationByAccessToken("access.jwt")).willReturn(false);

        // when
        filter.doFilterInternal(request, response, chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        then(chain).should().doFilter(request, response);
    }

}
