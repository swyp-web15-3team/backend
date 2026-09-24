package com.team3.user.exception;

import com.team3.common.exception.CustomException;
import com.team3.user.enums.UserErrorCode;

public class UserNotFoundException extends CustomException {

    public UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND);
    }
}
