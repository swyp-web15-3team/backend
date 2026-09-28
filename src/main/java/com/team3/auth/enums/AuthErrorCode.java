package com.team3.auth.enums;

import com.team3.common.exception.ErrorCodeDefinition;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCodeDefinition {
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),

    ALREADY_SIGNED_UP(HttpStatus.CONFLICT, "AUTH_002", "이미 가입이 완료된 사용자입니다."),

    INVALID_USER_ID(HttpStatus.BAD_REQUEST, "AUTH_004", "유효하지 않은 사용자 ID입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
