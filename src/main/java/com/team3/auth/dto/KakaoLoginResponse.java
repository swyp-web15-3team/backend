package com.team3.auth.dto;

public record KakaoLoginResponse(String accessToken, String refreshToken, boolean isNewUser) {
}
