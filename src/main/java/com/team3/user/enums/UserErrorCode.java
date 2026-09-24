package com.team3.user.enums;

import com.team3.common.exception.ErrorCodeDefinition;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCodeDefinition {
    USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "USER_001", "사용자를 찾을 수 없습니다."),

    DELETED_USER(HttpStatus.FORBIDDEN, "AUTH_003", "탈퇴한 사용자입니다."),

    ACTIVE_USER_REQUIRED(HttpStatus.FORBIDDEN, "AUTH_005", "가입이 완료된 활성 사용자만 접근할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
