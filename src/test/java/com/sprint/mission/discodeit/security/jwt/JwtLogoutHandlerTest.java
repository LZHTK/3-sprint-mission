package com.sprint.mission.discodeit.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.security.DiscodeitUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class JwtLogoutHandlerTest {

    @Mock private JwtRegistry jwtRegistry;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private Authentication authentication;
    @Mock private DiscodeitUserDetails userDetails;

    @InjectMocks private JwtLogoutHandler logoutHandler;

    @Test
    @DisplayName("쿠키와 인증정보를 활용해 JWT를 모두 무효화하고 쿠키를 즉시 삭제한다")
    void logout_쿠키와인증정보() {
        // given: refresh 쿠키와 인증 principal이 존재함
        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, "kim", "kim@sprint.io", Role.USER, null, true);
        Cookie refreshCookie = new Cookie("REFRESH_TOKEN", "refresh.jwt");
        given(request.getCookies()).willReturn(new Cookie[]{refreshCookie});
        given(request.isSecure()).willReturn(true);
        given(jwtTokenProvider.extractUserId("refresh.jwt")).willReturn(userId);
        given(authentication.getPrincipal()).willReturn(userDetails);
        given(userDetails.getUserDto()).willReturn(userDto);
        given(userDetails.getUsername()).willReturn("kim");
        given(authentication.getName()).willReturn("kim");

        // when: logout 처리
        logoutHandler.logout(request, response, authentication);

        // then: JwtRegistry가 두 경로 모두에서 호출되고 쿠키가 삭제됨
        then(jwtRegistry).should(times(2)).invalidateJwtInformationByUserId(userId);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        then(response).should().addCookie(cookieCaptor.capture());
        Cookie deletedCookie = cookieCaptor.getValue();
        assertThat(deletedCookie.getName()).isEqualTo("REFRESH_TOKEN");
        assertThat(deletedCookie.getValue()).isNull();
        assertThat(deletedCookie.getMaxAge()).isZero();
        assertThat(deletedCookie.getSecure()).isTrue();
    }

    @Test
    @DisplayName("인증 객체가 없어도 쿠키만으로 JWT를 무효화하고 쿠키를 삭제한다")
    void logout_cookieOnly() {
        // given
        Cookie refreshCookie = new Cookie("REFRESH_TOKEN", "refresh.jwt");
        UUID userId = UUID.randomUUID();
        given(request.getCookies()).willReturn(new Cookie[]{refreshCookie});
        given(jwtTokenProvider.extractUserId("refresh.jwt")).willReturn(userId);
        given(request.isSecure()).willReturn(false);

        // when
        logoutHandler.logout(request, response, null);

        // then
        then(jwtRegistry).should().invalidateJwtInformationByUserId(userId);
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        then(response).should().addCookie(cookieCaptor.capture());
        assertThat(cookieCaptor.getValue().getMaxAge()).isZero();
    }

    @Test
    @DisplayName("쿠키와 인증 정보가 모두 없으면 JwtRegistry만 건드리지 않는다")
    void logout_withoutCookieAndAuthentication() {
        // given: 쿠키가 없고 authentication 도 null
        given(request.getCookies()).willReturn(null);

        // when
        logoutHandler.logout(request, response, null);

        // then: Registry는 호출되지 않음
        then(jwtRegistry).shouldHaveNoInteractions();

        // and: 응답에는 만료된 쿠키가 한 번 추가됨
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        then(response).should().addCookie(cookieCaptor.capture());
        Cookie expired = cookieCaptor.getValue();
        assertThat(expired.getName()).isEqualTo("REFRESH_TOKEN");
        assertThat(expired.getMaxAge()).isZero();
    }

}

