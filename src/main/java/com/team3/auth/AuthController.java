package com.team3.auth;

import com.team3.user.Provider;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String STATE = AuthController.class.getName() + ".state";
    private final AuthService tokens;
    private final Optional<KakaoClient> kakao;
    private final Clock clock;

    public AuthController(AuthService tokens, Optional<KakaoClient> kakao, Clock clock) {
        this.tokens = tokens;
        this.kakao = kakao;
        this.clock = clock;
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthService.TokenPair> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(tokens.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        tokens.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/kakao")
    public ResponseEntity<Void> login(HttpServletRequest request) {
        KakaoClient client = kakaoClient();
        HttpSession session = request.getSession();
        request.changeSessionId();
        String state = UUID.randomUUID().toString();
        session.setAttribute(STATE, new LoginState(state, clock.instant().plusSeconds(300)));
        return ResponseEntity.status(HttpStatus.FOUND).location(client.authorizationUri(state))
            .cacheControl(CacheControl.noStore()).header("Referrer-Policy", "no-referrer").build();
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<AuthService.TokenPair> callback(HttpServletRequest request,
        @RequestParam(required = false) String state, @RequestParam(required = false) String code,
        @RequestParam(required = false) String error) {
        KakaoClient client = kakaoClient();
        HttpSession session = request.getSession(false);
        LoginState expected = null;
        if (session != null) {
            synchronized (session) {
                expected = (LoginState) session.getAttribute(STATE);
                session.removeAttribute(STATE);
            }
        }
        if (expected == null || !expected.value().equals(state) || !expected.expiresAt().isAfter(clock.instant())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OAuth state.");
        }
        if (error != null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kakao login denied.");
        }
        if (code == null || code.isBlank() || code.length() > 2048) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid authorization code.");
        }
        long kakaoId = client.userId(code);
        Long userId = tokens.findOrCreateUser(Provider.KAKAO, Long.toString(kakaoId));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Referrer-Policy", "no-referrer")
            .body(tokens.issue(userId));
    }

    private record LoginState(String value, Instant expiresAt) implements java.io.Serializable {
    }

    private KakaoClient kakaoClient() {
        return kakao.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kakao login is disabled."));
    }

    public record RefreshRequest(@NotBlank @Size(max = 128) String refreshToken) {
    }
}
