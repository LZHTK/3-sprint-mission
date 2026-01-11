package com.sprint.mission.discodeit.entity;

import java.util.Locale;

public enum SocialProvider {
    GOOGLE, KAKAO;

    public static SocialProvider from(String value) {
        return SocialProvider.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
