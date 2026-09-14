package com.team3.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."), COLLECTION_NAME_DUPLICATE(
        HttpStatus.CONFLICT, "COLLECTION_001", "이미 존재하는 관심 그룹 이름입니다."), COLLECTION_NOT_FOUND(HttpStatus.NOT_FOUND,
            "COLLECTION_002", "관심 그룹을 찾을 수 없습니다."), DEFAULT_COLLECTION_IMMUTABLE(HttpStatus.BAD_REQUEST,
                "COLLECTION_003", "기본 관심 그룹은 수정할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
