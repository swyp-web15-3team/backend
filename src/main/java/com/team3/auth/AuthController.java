package com.team3.auth;

import com.team3.auth.dto.KakaoLoginRequest;
import com.team3.auth.dto.KakaoLoginResponse;
import com.team3.auth.dto.LogoutRequest;
import com.team3.auth.dto.RefreshRequest;
import com.team3.auth.dto.RefreshResponse;
import com.team3.user.Provider;
import com.team3.common.ApiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService tokens;
    @Nullable private final KakaoClient kakao;

    public AuthController(AuthService tokens, @Nullable KakaoClient kakao) {
        this.tokens = tokens;
        this.kakao = kakao;
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthService.TokenPair pair = tokens.refresh(request.refreshToken());
        return ApiResponse.of(new RefreshResponse(pair.accessToken(), pair.refreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        tokens.logout(request.refreshToken());
    }

    @PostMapping("/kakao")
    public ApiResponse<KakaoLoginResponse> login(@Valid @RequestBody KakaoLoginRequest request) {
        long kakaoId = kakaoClient().userId(request.code());
        AuthService.UserResult user = tokens.findOrCreateUser(Provider.KAKAO, Long.toString(kakaoId));
        AuthService.TokenPair pair = tokens.issue(user.userId());
        return ApiResponse.of(
            new KakaoLoginResponse(pair.accessToken(), pair.refreshToken(), user.isNewUser()));
    }

    private KakaoClient kakaoClient() {
        if (kakao == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Kakao login is disabled.");
        }
        return kakao;
    }
}
