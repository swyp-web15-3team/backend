package com.team3.auth;

import com.team3.auth.jwt.JwtProvider;
import com.team3.auth.jwt.JwtConfig;
import com.team3.auth.token.RefreshTokenService;
import com.team3.auth.token.RefreshToken;
import com.team3.auth.token.RefreshTokenRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class AuthServiceTests {

    private static final String SECRET = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
    private final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    private final JwtConfig config = new JwtConfig();
    private final Clock clock = Clock.systemUTC();
    private AuthService service;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() {
        SecretKey key = config.jwtKey(SECRET);
        decoder = config.jwtDecoder(key, "backend");
        service = service(clock);
    }

    @Test
    void issuesSignedAccessTokenAndStoresOnlyRefreshHash() throws Exception {
        AuthService.TokenPair pair = service.issue(1L);
        Jwt jwt = decoder.decode(pair.accessToken());
        assertThat(jwt.getSubject()).isEqualTo("1");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
        assertThat(pair.expiresIn()).isEqualTo(900);
        assertThat(pair.refreshToken()).matches("[A-Za-z0-9_-]{43}");
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(ReflectionTestUtils.getField(saved.getValue(), "tokenHash"))
            .isInstanceOf(String.class).isNotEqualTo(pair.refreshToken())
            .isEqualTo(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(pair.refreshToken().getBytes(StandardCharsets.US_ASCII))));
        assertThat(saved.getValue().userId()).isEqualTo(1L);
    }

    @Test
    void rotatesRefreshTokenRejectsReuseAndSupportsLogout() {
        RefreshToken[] row = new RefreshToken[1];
        when(repository.save(any())).thenAnswer(invocation -> {
            row[0] = invocation.getArgument(0);
            return row[0];
        });
        when(repository.findByTokenHash(anyString()))
            .thenAnswer(invocation -> Optional.ofNullable(row[0]).filter(token -> invocation.getArgument(0)
                .equals(ReflectionTestUtils.getField(token, "tokenHash"))));
        AuthService.TokenPair original = service.issue(1L);
        Instant expiration = row[0].expiresAt();
        AuthService.TokenPair rotated = service.refresh(original.refreshToken());
        assertThat(rotated.refreshToken()).isNotEqualTo(original.refreshToken());
        assertThat(decoder.decode(rotated.accessToken()).getSubject()).isEqualTo("1");
        assertThat(row[0].expiresAt()).isEqualTo(expiration);
        assertUnauthorized(() -> service.refresh(original.refreshToken()));
        service.logout(rotated.refreshToken());
        verify(repository).delete(row[0]);
    }

    @Test
    void logoutRevokesOnlyTheSelectedSession() {
        List<RefreshToken> rows = new ArrayList<>();
        when(repository.save(any())).thenAnswer(invocation -> {
            RefreshToken row = invocation.getArgument(0);
            rows.add(row);
            return row;
        });
        when(repository.findByTokenHash(anyString())).thenAnswer(invocation -> rows.stream()
            .filter(row -> invocation.getArgument(0).equals(ReflectionTestUtils.getField(row, "tokenHash")))
            .findFirst());
        doAnswer(invocation -> rows.remove(invocation.getArgument(0))).when(repository).delete(any());
        AuthService.TokenPair first = service.issue(1L);
        AuthService.TokenPair second = service.issue(1L);
        service.logout(first.refreshToken());
        assertThat(rows).hasSize(1);
        assertUnauthorized(() -> service.refresh(first.refreshToken()));
        assertThat(decoder.decode(service.refresh(second.refreshToken()).accessToken()).getSubject()).isEqualTo("1");
    }

    @Test
    void rejectsExpiredUnknownAndMalformedRefreshTokens() {
        String raw = "a".repeat(43);
        assertUnauthorized(() -> service.refresh(raw));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(
            new RefreshToken(1L, "hash", clock.instant().minusSeconds(1))));
        assertUnauthorized(() -> service.refresh(raw));
        assertUnauthorized(() -> service.refresh("bad"));
        assertUnauthorized(() -> service.refresh(null));
    }

    @Test
    void rejectsExpiredAndIncorrectlySignedJwtAndWrongIssuer() {
        AuthService oldService = service(Clock.fixed(Instant.now().minusSeconds(3600), ZoneOffset.UTC));
        assertThatThrownBy(() -> decoder.decode(oldService.issue(1L).accessToken()))
            .isInstanceOf(JwtException.class);
        String access = service.issue(1L).accessToken();
        JwtDecoder wrongKey = config.jwtDecoder(config.jwtKey(
            "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="), "backend");
        assertThatThrownBy(() -> wrongKey.decode(access)).isInstanceOf(JwtException.class);
        JwtDecoder wrongIssuer = config.jwtDecoder(config.jwtKey(SECRET), "other");
        assertThatThrownBy(() -> wrongIssuer.decode(access)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsWeakKeysAndInvalidUserIds() {
        assertThatThrownBy(() -> config.jwtKey("YWJj")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.issue(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.issue(0L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.issue(-1L)).isInstanceOf(IllegalArgumentException.class);
    }

    private AuthService service(Clock tokenClock) {
        return new AuthService(new RefreshTokenService(repository, tokenClock,
            Duration.ofMinutes(15), Duration.ofDays(14)),
            new JwtProvider(config.jwtEncoder(config.jwtKey(SECRET)), tokenClock, "backend", Duration.ofMinutes(15)));
    }

    private void assertUnauthorized(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class,
            ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
