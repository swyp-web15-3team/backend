package com.team3.auth.jwt;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

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

    public JwtProvider(JwtEncoder encoder, Clock clock, JwtProperties properties) {
        this.encoder = encoder;
        this.clock = clock;
        this.issuer = properties.issuer();
        this.accessTtl = properties.accessTtl();
    }

    public String issue(Long userId) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject(userId.toString())
            .issuedAt(now).expiresAt(now.plus(accessTtl)).id(UUID.randomUUID().toString()).build();
        return encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
