package com.team3.common.exception;

import java.util.Map;

import lombok.Getter;

@Getter
public abstract class CustomException extends RuntimeException {
    private final ErrorCodeDefinition errorCode;
    private final Map<String, Object> properties;

    protected CustomException(ErrorCodeDefinition errorCode) {
        this(errorCode, Map.of());
    }

    protected CustomException(ErrorCodeDefinition errorCode, Map<String, Object> properties) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.properties = properties == null || properties.isEmpty() ? Map.of() : Map.copyOf(properties);
    }
}
