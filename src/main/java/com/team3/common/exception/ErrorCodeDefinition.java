package com.team3.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCodeDefinition {
    HttpStatus getStatus();

    String getCode();

    String getMessage();
}
