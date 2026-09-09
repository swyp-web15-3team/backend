package com.team3.auth.jwt;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private final JwtEncoder encoder;
    private final Clock clock;
    private final String issuer;
    private final Duration accessTtl;

    public JwtProvider(JwtEncoder encoder, Clock clock,
        @Value("${auth.jwt.issuer}") String issuer,
        @Value("${auth.jwt.access-ttl}") Duration accessTtl) {
        if (accessTtl.getSeconds() < 1) {
            throw new IllegalArgumentException("Access token lifetime must be positive.");
        }
        this.encoder = encoder;
        this.clock = clock;
        this.issuer = issuer;
        this.accessTtl = accessTtl;
    }

    public String issue(Long userId) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject(userId.toString())
            .issuedAt(now).expiresAt(now.plus(accessTtl)).id(UUID.randomUUID().toString()).build();
        return encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public long expiresIn() {
        return accessTtl.toSeconds();
    }
}
