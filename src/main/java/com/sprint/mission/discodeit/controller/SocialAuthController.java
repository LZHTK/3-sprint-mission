package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.config.OAuthProperties;
import com.sprint.mission.discodeit.service.SocialOAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/social")
public class SocialAuthController {

    private static final String STATE_COOKIE = "OAUTH_STATE";

    private final SocialOAuthService socialOAuthService;
    private final OAuthProperties oAuthProperties;

    @GetMapping("/redirect")
    public void redirect(@RequestParam String provider, HttpServletResponse response) throws Exception {
        String state = UUID.randomUUID().toString();

        Cookie cookie = new Cookie(STATE_COOKIE, state);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);

        response.sendRedirect(socialOAuthService.buildAuthorizationUri(provider,state).toString());
    }

    @GetMapping("/callback/{provider}")
    public void callback(
        @PathVariable String provider,
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String state,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws Exception {

        if (!isValidState(state, request)) {
            redirectError(response, "invalid_state");
            return;
        }

        if (code == null) {
            redirectError(response, "missing_code");
            return;
        }

        try {
            var result = socialOAuthService.handleCallback(provider, code);

            Cookie refreshCookie = new Cookie("REFRESH_TOKEN", result.refreshToken());
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(request.isSecure());
            refreshCookie.setPath("/");
            response.addCookie(refreshCookie);

            String token = URLEncoder.encode(result.accessToken(), StandardCharsets.UTF_8);
            String redirectUrl = oAuthProperties.getFrontendSuccessRedirect();
            response.sendRedirect(redirectUrl + "?token" + token);
        } catch (Exception ex) {
            redirectError(response, "oauth_failed");
        }
    }

    private boolean isValidState(String state, HttpServletRequest request) {
        if (state == null || request.getCookies() == null) {
            return false;
        }
        for (Cookie cookie : request.getCookies()) {
            if (STATE_COOKIE.equals(cookie.getName())) {
                return state.equals(cookie.getValue());
            }
        }
        return false;
    }

    private void redirectError(HttpServletResponse response, String error) throws Exception {
        String redirectUrl = oAuthProperties.getFrontendErrorRedirect();
        response.sendRedirect(redirectUrl + "?error=" + error);
    }
}
