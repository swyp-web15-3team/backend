package com.team3.auth;

import com.team3.user.User;
import com.team3.user.Provider;
import com.team3.user.UserRepository;

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

    public AuthService(RefreshTokenService refreshTokens, JwtProvider jwtProvider, UserRepository users) {
        this.refreshTokens = refreshTokens;
        this.jwtProvider = jwtProvider;
        this.users = users;
    }

    // Repository writes must finish their own rollback before a conflicting user
    // is read again.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Long findOrCreateUser(Provider provider, String providerId) {
        return users.findByProviderAndProviderId(provider, providerId).map(User::id).orElseGet(() -> {
            try {
                return users.saveAndFlush(new User(provider, providerId)).id();
            } catch (DataIntegrityViolationException ex) {
                return users.findByProviderAndProviderId(provider, providerId).map(User::id).orElseThrow(() -> ex);
            }
        });
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
        return new TokenPair(jwtProvider.issue(userId), refresh, jwtProvider.expiresIn());
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresIn) {
    }
}
