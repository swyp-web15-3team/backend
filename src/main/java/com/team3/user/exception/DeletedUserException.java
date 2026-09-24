package com.team3.user.exception;

import com.team3.common.exception.CustomException;
import com.team3.user.enums.UserErrorCode;

public class DeletedUserException extends CustomException {

    public DeletedUserException() {
        super(UserErrorCode.DELETED_USER);
    }
}
