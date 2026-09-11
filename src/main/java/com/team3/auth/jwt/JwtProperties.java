package com.team3.auth.jwt;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("auth.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTtl, Duration refreshTtl) {
    public JwtProperties {
        if (accessTtl == null || refreshTtl == null || accessTtl.getSeconds() < 1
            || refreshTtl.compareTo(accessTtl) <= 0) {
            throw new IllegalArgumentException("Token lifetimes must be positive, with refresh longer than access.");
        }
    }
}
