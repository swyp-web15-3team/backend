package com.team3.auth;

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

    private final AuthService tokens;

    public AuthController(AuthService tokens) {
        this.tokens = tokens;
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

    public record RefreshRequest(@NotBlank @Size(max = 128) String refreshToken) {
    }
}
