package com.team3.auth;

import com.team3.auth.jwt.JwtProvider;
import com.team3.auth.token.RefreshTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final RefreshTokenService refreshTokens;
    private final JwtProvider jwtProvider;

    public AuthService(RefreshTokenService refreshTokens, JwtProvider jwtProvider) {
        this.refreshTokens = refreshTokens;
        this.jwtProvider = jwtProvider;
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
