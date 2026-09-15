package com.team3.auth;

import com.team3.common.exception.CustomException;
import com.team3.common.exception.ErrorCode;

public class AlreadySignedUpException extends CustomException {
    public AlreadySignedUpException() {
        super(ErrorCode.ALREADY_SIGNED_UP);
    }
}
