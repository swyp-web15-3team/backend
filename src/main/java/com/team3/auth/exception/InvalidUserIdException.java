package com.team3.auth.exception;

import com.team3.common.exception.CustomException;
import com.team3.common.exception.ErrorCode;

public class InvalidUserIdException extends CustomException {
    public InvalidUserIdException() {
        super(ErrorCode.INVALID_USER_ID);
    }
}
