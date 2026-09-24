package com.team3.user;

import com.team3.common.ApiResponse;
import com.team3.user.dto.UserResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.of(users.getMe(Long.valueOf(jwt.getSubject())));
    }

    @PutMapping("/me/profile")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<UserResponse> update(@AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody ProfileRequest request) {
        return ApiResponse.of(users.updateNickname(Long.valueOf(jwt.getSubject()), request.nickname()));
    }

    public record ProfileRequest(
        @NotBlank @Size(max = 30) String nickname) {
    }
}
