package com.team3.auth.exception;

import com.team3.common.exception.CustomException;
import com.team3.auth.enums.AuthErrorCode;

public class InvalidUserIdException extends CustomException {
    public InvalidUserIdException() {
        super(AuthErrorCode.INVALID_USER_ID);
    }
}
