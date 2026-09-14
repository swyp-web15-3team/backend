package com.team3.common.exception;

import lombok.Getter;

@Getter
public abstract class CustomException extends RuntimeException {
    private final ErrorCode errorCode;

    protected CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
