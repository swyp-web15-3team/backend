package com.team3.auth;

import com.team3.user.AgreementType;
import com.team3.user.User;
import com.team3.user.Provider;
import com.team3.user.UserAgreement;
import com.team3.user.UserAgreementRepository;
import com.team3.user.UserRepository;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
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

    public AuthService(RefreshTokenService refreshTokens, JwtProvider jwtProvider, UserRepository users,
        UserAgreementRepository agreements) {
        this.refreshTokens = refreshTokens;
        this.jwtProvider = jwtProvider;
        this.users = users;
        this.agreements = agreements;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserResult findOrCreateUser(Provider provider, String providerId) {
        User user = users.findByProviderAndProviderId(provider, providerId).orElse(null);
        if (user != null) {
            return new UserResult(user.id(), user.isPending());
        }

        try {
            User created = users.saveAndFlush(new User(provider, providerId));
            return new UserResult(created.id(), created.isPending());
        } catch (DataIntegrityViolationException ex) {
            User existing = users.findByProviderAndProviderId(provider, providerId).orElse(null);
            if (existing != null) {
                return new UserResult(existing.id(), existing.isPending());
            }
            throw ex;
        }
    }

    public void signUp(Long userId, boolean marketingAgreed) {
        User user = users.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user."));
        if (!user.isPending()) {
            throw new AlreadySignedUpException();
        }
        user.activate();
        agreements.saveAll(List.of(
            new UserAgreement(userId, AgreementType.TERMS_OF_SERVICE, true),
            new UserAgreement(userId, AgreementType.PRIVACY_POLICY, true),
            new UserAgreement(userId, AgreementType.MARKETING, marketingAgreed)));
    }

    public TokenPair issue(Long userId) {
        String refresh = refreshTokens.issue(userId);
        return tokens(userId, refresh);
    }

    public TokenPair refresh(String rawToken) {
        RefreshTokenService.Rotation rotated = refreshTokens.refresh(rawToken);
        return tokens(rotated.userId(), rotated.refreshToken());
    }

    public void logout(String rawToken) {
        refreshTokens.logout(rawToken);
    }

    private TokenPair tokens(Long userId, String refresh) {
        return new TokenPair(jwtProvider.issue(userId), refresh);
    }

    public record TokenPair(String accessToken, String refreshToken) {
    }

    public record UserResult(Long userId, boolean isNewUser) {
    }
}
