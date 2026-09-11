package com.team3.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KakaoLoginRequest(@NotBlank @Size(max = 2048) String code) {
}
