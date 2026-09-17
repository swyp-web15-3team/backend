package com.team3.auth;

import com.team3.auth.exception.AlreadySignedUpException;
import com.team3.auth.exception.DeletedUserException;
import com.team3.auth.exception.InvalidUserIdException;
import com.team3.common.exception.ErrorCode;
import com.team3.user.AgreementType;
import com.team3.user.User;
import com.team3.user.Provider;
import com.team3.user.UserAgreement;
import com.team3.user.UserAgreementRepository;
import com.team3.user.UserRepository;

import com.team3.auth.jwt.JwtProvider;
import com.team3.auth.jwt.JwtConfig;
import com.team3.auth.jwt.JwtProperties;
import com.team3.auth.token.RefreshTokenService;
import com.team3.auth.token.RefreshToken;
import com.team3.auth.token.RefreshTokenRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

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
    private final UserRepository users = mock(UserRepository.class);
    private final UserAgreementRepository agreements = mock(UserAgreementRepository.class);
    private final RefreshTokenRepository.TokenOwner owner = () -> 1L;
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final TransactionStatus transaction = mock(TransactionStatus.class);
    private final JwtConfig config = new JwtConfig();
    private final Clock clock = Clock.systemUTC();
    private AuthService service;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() {
        SecretKey key = config.jwtKey(properties(SECRET, "backend"));
        decoder = config.jwtDecoder(key, properties(SECRET, "backend"));
        User user = new User(Provider.KAKAO, "123");
        when(users.findById(anyLong())).thenReturn(Optional.of(user));
        when(users.findLockedById(anyLong())).thenReturn(Optional.of(user));
        when(repository.findOwnerByTokenHash(anyString())).thenReturn(Optional.of(owner));
        when(transactionManager.getTransaction(any())).thenReturn(transaction);
        service = service(clock);
    }

    @Test
    void issuesSignedAccessTokenAndStoresOnlyRefreshHash() throws Exception {
        AuthService.TokenPair pair = service.issue(1L);
        Jwt jwt = decoder.decode(pair.accessToken());
        assertThat(jwt.getSubject()).isEqualTo("1");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
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
        org.mockito.InOrder order = inOrder(repository, users);
        order.verify(repository).findOwnerByTokenHash(anyString());
        order.verify(users).findLockedById(1L);
        order.verify(repository).findByTokenHash(anyString());
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
    void rejectsExpiredDisappearedMismatchedUnknownAndMalformedRefreshTokens() {
        String raw = "a".repeat(43);
        when(repository.findOwnerByTokenHash(anyString())).thenReturn(Optional.empty(), Optional.of(owner));
        assertUnauthorized(() -> service.refresh(raw));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(
            new RefreshToken(1L, "hash", clock.instant().minusSeconds(1))), Optional.empty(),
            Optional.of(
                new RefreshToken(2L, "hash", clock.instant().plusSeconds(1))));
        assertUnauthorized(() -> service.refresh(raw));
        assertUnauthorized(() -> service.refresh(raw));
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
            properties("AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=", "backend")), properties(SECRET, "backend"));
        assertThatThrownBy(() -> wrongKey.decode(access)).isInstanceOf(JwtException.class);
        JwtDecoder wrongIssuer = config.jwtDecoder(config.jwtKey(properties(SECRET, "backend")),
            properties(SECRET, "other"));
        assertThatThrownBy(() -> wrongIssuer.decode(access)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsWeakKeysAndInvalidUserIds() {
        assertThatThrownBy(() -> config.jwtKey(properties("YWJj", "backend")))
            .isInstanceOf(IllegalArgumentException.class);
        assertInvalidUserId(() -> service.issue(null));
        assertInvalidUserId(() -> service.issue(0L));
        assertInvalidUserId(() -> service.issue(-1L));
        assertInvalidUserId(() -> new RefreshTokenService(repository, clock, properties(SECRET, "backend")).issue(0L));
    }

    @Test
    void createsUserAndRecoversConcurrentCreationConflict() {
        Long id = 42L;
        User user = mock(User.class);
        when(user.id()).thenReturn(id);
        when(user.isPending()).thenReturn(true);
        when(users.saveAndFlush(any(User.class))).thenReturn(user);
        assertThat(service.findOrCreateUser(Provider.KAKAO, "external:abc-123"))
            .isEqualTo(new AuthService.UserResult(id, true));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(saved.capture());
        assertThat(ReflectionTestUtils.getField(saved.getValue(), "provider")).isEqualTo(Provider.KAKAO);
        assertThat(ReflectionTestUtils.getField(saved.getValue(), "providerId")).isEqualTo("external:abc-123");

        DataIntegrityViolationException conflict = new DataIntegrityViolationException("duplicate");
        when(users.saveAndFlush(any(User.class))).thenThrow(conflict);
        when(users.findByProviderAndProviderId(Provider.KAKAO, "external:def-456")).thenReturn(Optional.empty())
            .thenReturn(Optional.of(user));
        assertThat(service.findOrCreateUser(Provider.KAKAO, "external:def-456"))
            .isEqualTo(new AuthService.UserResult(id, true));
        assertThatThrownBy(() -> service.findOrCreateUser(Provider.KAKAO, "external:ghi-789")).isSameAs(conflict);
    }

    @Test
    void reportsExistingUserAsNewUntilSignUpCompletes() {
        User pending = new User(Provider.KAKAO, "external:abc-123");
        when(users.findByProviderAndProviderId(Provider.KAKAO, "external:abc-123")).thenReturn(Optional.of(pending));
        assertThat(service.findOrCreateUser(Provider.KAKAO, "external:abc-123").isNewUser()).isTrue();

        when(users.findLockedById(7L)).thenReturn(Optional.of(pending));
        service.signUp(7L, true);
        assertThat(pending.isPending()).isFalse();

        ArgumentCaptor<List<UserAgreement>> saved = agreementsCaptor();
        verify(agreements).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(UserAgreement::type, UserAgreement::agreed).containsExactly(
            tuple(AgreementType.TERMS_OF_SERVICE, true), tuple(AgreementType.PRIVACY_POLICY, true),
            tuple(AgreementType.MARKETING, true));

        assertThat(service.findOrCreateUser(Provider.KAKAO, "external:abc-123").isNewUser()).isFalse();
        assertThatThrownBy(() -> service.signUp(7L, true)).isInstanceOf(AlreadySignedUpException.class);
    }

    @Test
    void rejectsSignUpForUnknownUser() {
        when(users.findLockedById(9L)).thenReturn(Optional.empty());
        assertUnauthorized(() -> service.signUp(9L, false));
    }

    @Test
    void rejectsDeletedUsersFromLoginSignUpTokenIssuanceAndRefresh() {
        User deleted = new User(Provider.KAKAO, "123");
        deleted.delete(clock.instant());
        when(users.findByProviderAndProviderId(Provider.KAKAO, "123")).thenReturn(Optional.of(deleted));
        when(users.findById(1L)).thenReturn(Optional.of(deleted));
        when(users.findLockedById(1L)).thenReturn(Optional.of(deleted));
        when(repository.findOwnerByTokenHash(anyString())).thenReturn(Optional.of(owner));
        assertThatThrownBy(() -> service.findOrCreateUser(Provider.KAKAO, "123"))
            .isInstanceOf(DeletedUserException.class);
        assertThatThrownBy(() -> service.signUp(1L, false)).isInstanceOf(DeletedUserException.class);
        assertThatThrownBy(() -> service.issue(1L)).isInstanceOf(DeletedUserException.class);
        assertThatThrownBy(() -> service.refresh("a".repeat(43))).isInstanceOf(DeletedUserException.class);
    }

    @Test
    void withdrawsAtClockInstantRevokesTokensOnceAndCommitsAfterKakaoUnlink() {
        Clock nonUtcClock = Clock.fixed(Instant.parse("2026-09-16T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        KakaoClient kakao = mock(KakaoClient.class);
        User user = new User(Provider.KAKAO, "123");
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(users.findLockedById(1L)).thenReturn(Optional.of(user));

        service(nonUtcClock).withdraw(1L, kakao);
        service(nonUtcClock).withdraw(1L, kakao);

        assertThat(user.deletedAt()).isEqualTo(nonUtcClock.instant());
        verify(repository, times(1)).deleteByUserId(1L);
        verify(transactionManager, times(1)).commit(transaction);
        org.mockito.InOrder order = inOrder(kakao, transactionManager);
        order.verify(kakao).unlink("123");
        order.verify(transactionManager).getTransaction(any());
    }

    @Test
    void doesNotStartFinalizationTransactionWhenKakaoUnlinkFails() {
        KakaoClient kakao = mock(KakaoClient.class);
        doThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kakao unlink failed.")).when(kakao).unlink("123");

        assertThatThrownBy(() -> service.withdraw(1L, kakao)).isInstanceOf(ResponseStatusException.class);

        verifyNoInteractions(transactionManager);
    }

    @Test
    void rollsBackFinalizationTransactionWhenTokenRevocationFails() {
        KakaoClient kakao = mock(KakaoClient.class);
        DataIntegrityViolationException failure = new DataIntegrityViolationException("database unavailable");
        doThrow(failure).when(repository).deleteByUserId(1L);

        assertThatThrownBy(() -> service.withdraw(1L, kakao)).isSameAs(failure);

        verify(transactionManager).rollback(transaction);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<UserAgreement>> agreementsCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    private AuthService service(Clock tokenClock) {
        return new AuthService(new RefreshTokenService(repository, tokenClock, properties(SECRET, "backend")),
            new JwtProvider(config.jwtEncoder(config.jwtKey(properties(SECRET, "backend"))), tokenClock,
                properties(SECRET, "backend")),
            users, agreements, transactionManager, tokenClock);
    }

    private JwtProperties properties(String secret, String issuer) {
        return new JwtProperties(secret, issuer, Duration.ofMinutes(15), Duration.ofDays(14));
    }

    private void assertUnauthorized(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class,
            ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    private void assertInvalidUserId(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(InvalidUserIdException.class, ex -> {
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_USER_ID);
            assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getErrorCode().getCode()).isEqualTo("AUTH_004");
        });
    }
}
