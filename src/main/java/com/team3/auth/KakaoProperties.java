package com.team3.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("auth.kakao")
public record KakaoProperties(boolean enabled, String clientId, String clientSecret, String redirectUri) {
}
