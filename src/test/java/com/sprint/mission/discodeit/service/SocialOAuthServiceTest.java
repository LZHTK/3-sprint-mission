package com.sprint.mission.discodeit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.mission.discodeit.config.OAuthProperties;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.SocialAccount;
import com.sprint.mission.discodeit.entity.SocialProvider;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.SocialAccountRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.security.jwt.JwtInformation;
import com.sprint.mission.discodeit.security.jwt.JwtRegistry;
import com.sprint.mission.discodeit.security.jwt.JwtTokenProvider;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("SocialOAuthService 단위 테스트")
public class SocialOAuthServiceTest {

    @Mock private RestTemplateBuilder restTemplateBuilder;
    @Mock private RestTemplate restTemplate;
    @Mock private SocialAccountRepository socialAccountRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private JwtRegistry jwtRegistry;

    private OAuthProperties oAuthProperties;
    private SocialOAuthService service;

    @BeforeEach
    void setUp() {
        // given
        oAuthProperties = new OAuthProperties();
        addProvider("google");
        addProvider("kakao");

        service = new SocialOAuthService(
            oAuthProperties,
            restTemplateBuilder,
            socialAccountRepository,
            userRepository,
            userMapper,
            passwordEncoder,
            jwtTokenProvider,
            jwtRegistry
        );

        // lenient: 일부 테스트에서 사용하지 않는 스텁 허용
        Mockito.lenient().when(restTemplateBuilder.build()).thenReturn(restTemplate);
    }

    @Test
    @DisplayName("buildAuthorizationUri가 OAuth 파라미터를 포함한다")
    void buildAuthorizationUri_shouldIncludeAllParams() {
        // when
        URI uri = service.buildAuthorizationUri("google", "state-123");

        // then
        MultiValueMap<String, String> params =
            UriComponentsBuilder.fromUri(uri).build(true).getQueryParams();

        assertThat(params.getFirst("response_type")).isEqualTo("code");
        assertThat(params.getFirst("client_id")).isEqualTo("google-client");

        String redirectUri = UriUtils.decode(params.getFirst("redirect_uri"), StandardCharsets.UTF_8);
        assertThat(redirectUri).isEqualTo("https://app.example.com/oauth");

        String scope = UriUtils.decode(params.getFirst("scope"), StandardCharsets.UTF_8);
        assertThat(scope).isEqualTo("openid profile email");

        assertThat(params.getFirst("state")).isEqualTo("state-123");
    }

    @Test
    @DisplayName("handleCallback이 기존 소셜 계정을 사용하고 JWT를 등록한다")
    void handleCallback_shouldUseExistingAccount() {
        // given
        Map<String, Object> tokenResponse = Map.of("access_token", "oauth-access");
        Map<String, Object> userInfo = Map.of(
            "sub", "google-123",
            "email", "user@example.com",
            "name", "Test User"
        );

        given(restTemplate.postForObject(
            eq("https://oauth.example.com/token"), any(HttpEntity.class), eq(Map.class)
        )).willReturn(tokenResponse);
        given(restTemplate.exchange(
            eq("https://oauth.example.com/userinfo"), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(Map.class)
        )).willReturn(ResponseEntity.ok(userInfo));

        User user = new User("test", "user@example.com", "pw", null);
        SocialAccount socialAccount = new SocialAccount(SocialProvider.GOOGLE, "google-123", user);
        given(socialAccountRepository.findByProviderAndProviderUserIdWithUser(
            SocialProvider.GOOGLE, "google-123"
        )).willReturn(Optional.of(socialAccount));

        UserDto dto = new UserDto(UUID.randomUUID(), "test", "user@example.com", Role.USER, null, false);
        given(userMapper.toDto(user)).willReturn(dto);

        given(jwtTokenProvider.generateAccessToken(any(Authentication.class))).willReturn("access");
        given(jwtTokenProvider.generateRefreshToken(any(Authentication.class))).willReturn("refresh");

        // when
        SocialOAuthService.SocialLoginResult result =
            service.handleCallback("google", "code-1");

        // then
        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isEqualTo("refresh");
        assertThat(result.userDto()).isEqualTo(dto);

        ArgumentCaptor<JwtInformation> jwtCaptor = ArgumentCaptor.forClass(JwtInformation.class);
        then(jwtRegistry).should().registerJwtInformation(jwtCaptor.capture());
        assertThat(jwtCaptor.getValue().getUserDto()).isEqualTo(dto);
        assertThat(jwtCaptor.getValue().getAccessToken()).isEqualTo("access");
        assertThat(jwtCaptor.getValue().getRefreshToken()).isEqualTo("refresh");
    }

    @Test
    @DisplayName("계정이 없고 이메일이 비어 있으면 사용자를 생성한다")
    void handleCallback_shouldCreateUserWhenEmailMissing() {
        // given
        Map<String, Object> tokenResponse = Map.of("access_token", "oauth-access");
        Map<String, Object> account = new HashMap<>();
        account.put("email", "");
        account.put("profile", Map.of("nickname", "Nick Name"));
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", "999");
        userInfo.put("kakao_account", account);

        given(restTemplate.postForObject(
            eq("https://oauth.example.com/token"), any(HttpEntity.class), eq(Map.class)
        )).willReturn(tokenResponse);
        given(restTemplate.exchange(
            eq("https://oauth.example.com/userinfo"), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(Map.class)
        )).willReturn(ResponseEntity.ok(userInfo));

        given(socialAccountRepository.findByProviderAndProviderUserIdWithUser(
            SocialProvider.KAKAO, "999"
        )).willReturn(Optional.empty());

        given(userRepository.findByEmail("kakao_999@social.local")).willReturn(Optional.empty());
        given(userRepository.existsByUsername("NickName")).willReturn(false);
        given(passwordEncoder.encode(any(String.class))).willReturn("encoded");

        User saved = new User("NickName", "kakao_999@social.local", "encoded", null);
        given(userRepository.save(any(User.class))).willReturn(saved);

        UserDto dto = new UserDto(UUID.randomUUID(), "NickName", "kakao_999@social.local", Role.USER, null, false);
        given(userMapper.toDto(saved)).willReturn(dto);

        given(jwtTokenProvider.generateAccessToken(any(Authentication.class))).willReturn("access");
        given(jwtTokenProvider.generateRefreshToken(any(Authentication.class))).willReturn("refresh");

        // when
        service.handleCallback("kakao", "code-2");

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("kakao_999@social.local");
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("NickName");

        then(socialAccountRepository).should().save(any(SocialAccount.class));
    }

    @Test
    @DisplayName("지원하지 않는 provider면 예외를 던진다")
    void buildAuthorizationUri_shouldThrowWhenProviderMissing() {
        // when / then
        assertThatThrownBy(() -> service.buildAuthorizationUri("unknown", "state"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private void addProvider(String name) {
        OAuthProperties.Provider provider = new OAuthProperties.Provider();
        provider.setClientId(name + "-client");
        provider.setClientSecret("secret");
        provider.setAuthUrl("https://oauth.example.com/authorize");
        provider.setTokenUrl("https://oauth.example.com/token");
        provider.setUserInfoUrl("https://oauth.example.com/userinfo");
        provider.setRedirectUrl("https://app.example.com/oauth");
        provider.setScope("openid profile email");
        oAuthProperties.getProviders().put(name, provider);
    }
}
