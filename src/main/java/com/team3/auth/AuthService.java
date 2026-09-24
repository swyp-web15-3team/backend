package com.team3.auth;

import com.team3.auth.exception.AlreadySignedUpException;
import com.team3.user.exception.DeletedUserException;
import com.team3.user.exception.UserNotFoundException;
import com.team3.auth.exception.InvalidUserIdException;
import com.team3.user.enums.AgreementType;
import com.team3.user.User;
import com.team3.user.enums.Provider;
import com.team3.user.UserAgreement;
import com.team3.user.UserAgreementRepository;
import com.team3.user.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import com.team3.auth.jwt.JwtProvider;
import com.team3.auth.token.RefreshTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final RefreshTokenService refreshTokens;
    private final JwtProvider jwtProvider;
    private final UserRepository users;
    private final UserAgreementRepository agreements;
    private final TransactionTemplate withdrawalTransaction;
    private final Clock clock;

    public AuthService(RefreshTokenService refreshTokens, JwtProvider jwtProvider, UserRepository users,
        UserAgreementRepository agreements, PlatformTransactionManager transactionManager, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.jwtProvider = jwtProvider;
        this.users = users;
        this.agreements = agreements;
        this.withdrawalTransaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserResult findOrCreateUser(Provider provider, String providerId) {
        User user = users.findByProviderAndProviderId(provider, providerId).orElse(null);
        if (user != null) {
            checkNotDeleted(user);
            return new UserResult(user.id(), user.isPending());
        }

        try {
            User created = users.saveAndFlush(new User(provider, providerId));
            return new UserResult(created.id(), created.isPending());
        } catch (DataIntegrityViolationException ex) {
            User existing = users.findByProviderAndProviderId(provider, providerId).orElse(null);
            if (existing != null) {
                checkNotDeleted(existing);
                return new UserResult(existing.id(), existing.isPending());
            }
            throw ex;
        }
    }

    public void signUp(Long userId, boolean marketingAgreed, String nickname) {
        User user = users.findLockedById(userId)
            .orElseThrow(UserNotFoundException::new);
        checkNotDeleted(user);
        if (!user.isPending()) {
            throw new AlreadySignedUpException();
        }
        user.updateNickname(nickname);
        user.activate();
        agreements.saveAll(List.of(
            new UserAgreement(userId, AgreementType.AGE_OVER_14, true),
            new UserAgreement(userId, AgreementType.TERMS_OF_SERVICE, true),
            new UserAgreement(userId, AgreementType.PRIVACY_POLICY, true),
            new UserAgreement(userId, AgreementType.MARKETING, marketingAgreed)));
    }

    public TokenPair issue(Long userId, String profileImageUrl) {
        if (userId == null || userId <= 0) {
            throw new InvalidUserIdException();
        }
        User user = users.findLockedById(userId)
            .orElseThrow(UserNotFoundException::new);
        checkNotDeleted(user);
        user.updateProfileImageUrl(profileImageUrl);
        String refresh = refreshTokens.issue(userId);
        return tokens(userId, refresh);
    }

    public TokenPair refresh(String rawToken) {
        Long userId = refreshTokens.userId(rawToken);
        User user = users.findLockedById(userId)
            .orElseThrow(UserNotFoundException::new);
        checkNotDeleted(user);
        RefreshTokenService.Rotation rotated = refreshTokens.refresh(rawToken, userId);
        return tokens(rotated.userId(), rotated.refreshToken());
    }

    public void logout(String rawToken) {
        refreshTokens.logout(rawToken);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void withdraw(Long userId, KakaoClient kakao) {
        Instant startedAt = clock.instant();
        User user = users.findById(userId)
            .orElseThrow(UserNotFoundException::new);
        if (user.isDeleted()) {
            return;
        }
        String providerId = user.providerId();
        kakao.unlink(providerId);
        withdrawalTransaction.executeWithoutResult(status -> finalizeWithdrawal(userId, startedAt));
    }

    private void finalizeWithdrawal(Long userId, Instant startedAt) {
        User user = users.findLockedById(userId)
            .orElseThrow(UserNotFoundException::new);
        if (!user.isDeleted()) {
            user.delete(startedAt);
            refreshTokens.revokeAll(userId);
        }
    }

    private void checkNotDeleted(User user) {
        if (user.isDeleted()) {
            throw new DeletedUserException();
        }
    }

    private TokenPair tokens(Long userId, String refresh) {
        return new TokenPair(jwtProvider.issue(userId), refresh);
    }

    public record TokenPair(String accessToken, String refreshToken) {
    }

    public record UserResult(Long userId, boolean isNewUser) {
    }
}
