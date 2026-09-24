package com.team3.user.exception;

import com.team3.common.exception.CustomException;
import com.team3.user.enums.UserErrorCode;

public class ActiveUserRequiredException extends CustomException {

    public ActiveUserRequiredException() {
        super(UserErrorCode.ACTIVE_USER_REQUIRED);
    }
}
