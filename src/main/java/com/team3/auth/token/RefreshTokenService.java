package com.team3.auth.token;

import com.team3.auth.jwt.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final Clock clock;
    private final Duration refreshTtl;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository, Clock clock, JwtProperties properties) {
        this.repository = repository;
        this.clock = clock;
        this.refreshTtl = properties.refreshTtl();
    }

    public String issue(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("A positive user ID is required.");
        }
        String refresh = newRefreshToken();
        repository.save(new RefreshToken(userId, hash(refresh), clock.instant().plus(refreshTtl)));
        return refresh;
    }

    public Rotation refresh(String rawToken) {
        RefreshToken stored = repository.findByTokenHash(hash(rawToken))
            .filter(token -> token.expiresAt().isAfter(clock.instant()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token."));
        String refresh = newRefreshToken();
        stored.rotate(hash(refresh));
        return new Rotation(stored.userId(), refresh);
    }

    public void logout(String rawToken) {
        repository.findByTokenHash(hash(rawToken)).ifPresent(repository::delete);
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        if (rawToken == null || !rawToken.matches("[A-Za-z0-9_-]{43}")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token.");
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(rawToken.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    public record Rotation(Long userId, String refreshToken) {
    }
}
