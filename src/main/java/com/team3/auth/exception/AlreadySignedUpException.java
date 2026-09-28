package com.team3.auth.exception;

import com.team3.common.exception.CustomException;
import com.team3.auth.enums.AuthErrorCode;

public class AlreadySignedUpException extends CustomException {
    public AlreadySignedUpException() {
        super(AuthErrorCode.ALREADY_SIGNED_UP);
    }
}
