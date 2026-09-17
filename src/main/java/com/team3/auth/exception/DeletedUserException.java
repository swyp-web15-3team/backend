package com.team3.auth.exception;

import com.team3.common.exception.CustomException;
import com.team3.common.exception.ErrorCode;

public class DeletedUserException extends CustomException {

    public DeletedUserException() {
        super(ErrorCode.DELETED_USER);
    }
}
