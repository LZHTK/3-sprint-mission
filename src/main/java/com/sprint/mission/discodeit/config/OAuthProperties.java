package com.sprint.mission.discodeit.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {

    private String frontendSuccessRedirect;
    private String frontendErrorRedirect;

    private Map<String, Provider> providers = new HashMap<>();

    @Getter
    @Setter
    public static class Provider {
        private String clientId;
        private String clientSecret;
        private String authUrl;
        private String tokenUrl;
        private String userInfoUrl;
        private String redirectUrl;
        private String scope;
    }
}
