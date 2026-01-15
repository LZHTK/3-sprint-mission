package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.config.OAuthProperties;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.SocialAccount;
import com.sprint.mission.discodeit.entity.SocialProvider;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.SocialAccountRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.security.DiscodeitUserDetails;
import com.sprint.mission.discodeit.security.jwt.JwtInformation;
import com.sprint.mission.discodeit.security.jwt.JwtRegistry;
import com.sprint.mission.discodeit.security.jwt.JwtTokenProvider;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class SocialOAuthService {

    private final OAuthProperties oAuthProperties;
    private final RestTemplateBuilder restTemplateBuilder;
    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtRegistry jwtRegistry;

    public URI buildAuthorizationUri(String provider, String state) {
        OAuthProperties.Provider config = getProvider(provider);

        return UriComponentsBuilder.fromUriString(config.getAuthUrl())
            .queryParam("response_type", "code")
            .queryParam("client_id", config.getClientId())
            .queryParam("redirect_uri", config.getRedirectUrl())
            .queryParam("scope", config.getScope())
            .queryParam("state", state)
            .build()
            .encode(StandardCharsets.UTF_8)
            .toUri();
    }

    public SocialLoginResult handleCallback(String provider, String code) {
        OAuthUserInfo userInfo = fetchUserInfo(provider, code);
        User user = findOrCreateUser(userInfo);
        UserDto userDto = userMapper.toDto(user);

        DiscodeitUserDetails userDetails =
            new DiscodeitUserDetails(userDto, user.getPassword(), user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities()
        );

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);
        jwtRegistry.registerJwtInformation(new JwtInformation(userDto, accessToken, refreshToken));

        return new SocialLoginResult(userDto, accessToken, refreshToken);
    }

    private OAuthUserInfo fetchUserInfo(String provider, String code) {
        OAuthProperties.Provider config = getProvider(provider);
        RestTemplate restTemplate = restTemplateBuilder.build();

        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> tokenBody = new LinkedMultiValueMap<>();
        tokenBody.add("grant_type", "authorization_code");
        tokenBody.add("code", code);
        tokenBody.add("redirect_uri", config.getRedirectUrl());
        tokenBody.add("client_id", config.getClientId());
        if (StringUtils.hasText(config.getClientSecret())) {
            tokenBody.add("client_secret", config.getClientSecret());
        }

        HttpEntity<MultiValueMap<String, String>> tokenRequest =
            new HttpEntity<>(tokenBody, tokenHeaders);

        Map<String, Object> tokenResponse = restTemplate.postForObject(
            config.getTokenUrl(), tokenRequest, Map.class
        );
        String accessToken = (String) tokenResponse.get("access_token");

        HttpHeaders userInfoHeaders = new HttpHeaders();
        userInfoHeaders.setBearerAuth(accessToken);

        ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
            config.getUserInfoUrl(),
            HttpMethod.GET,
            new HttpEntity<>(userInfoHeaders),
            Map.class
        );

        return mapUserInfo(provider, userInfoResponse.getBody());
    }

    private OAuthUserInfo mapUserInfo(String provider, Map<String, Object> body) {
        SocialProvider socialProvider = SocialProvider.from(provider);

        if (socialProvider == SocialProvider.GOOGLE) {
            String id = (String) body.get("sub"); // Google은 sub가 고유 ID
            String email = (String) body.get("email");
            String name = (String) body.get("name");
            return new OAuthUserInfo(socialProvider, id, email, name);
        }

        // KAKAO
        String id = String.valueOf(body.get("id"));
        Map<String, Object> account = (Map<String, Object>) body.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");
        String email = (String) account.get("email");
        String nickname = (String) profile.get("nickname");
        return new OAuthUserInfo(socialProvider, id, email, nickname);
    }

    private User findOrCreateUser(OAuthUserInfo info) {
        SocialAccount existingAccount =
            socialAccountRepository.findByProviderAndProviderUserId(
                info.provider(), info.providerUserId()
            ).orElse(null);

        if (existingAccount != null) {
            return existingAccount.getUser();
        }

        if (!StringUtils.hasText(info.email())) {
            throw new IllegalStateException("Email is required for social login.");
        }

        User user = userRepository.findByEmail(info.email())
            .orElseGet(() -> createUser(info));

        socialAccountRepository.save(new SocialAccount(
            info.provider(), info.providerUserId(), user
        ));

        return user;
    }

    private User createUser(OAuthUserInfo info) {
        String baseUsername = StringUtils.hasText(info.username())
            ? info.username().replaceAll("\\s+", "")
            : "user";
        String username = ensureUniqueUsername(baseUsername);
        String password = passwordEncoder.encode(UUID.randomUUID().toString());

        User user = new User(username, info.email(), password, null);
        return userRepository.save(user);
    }

    private String ensureUniqueUsername(String base) {
        String candidate = base;
        if (!userRepository.existsByUsername(candidate)) {
            return candidate;
        }
        return base + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private OAuthProperties.Provider getProvider(String provider) {
        OAuthProperties.Provider config = oAuthProperties.getProviders().get(provider);
        if (config == null) {
            throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
        return config;
    }

    public record SocialLoginResult(UserDto userDto, String accessToken, String refreshToken) {}

    public record OAuthUserInfo(
        SocialProvider provider,
        String providerUserId,
        String email,
        String username
    ) {}
}
