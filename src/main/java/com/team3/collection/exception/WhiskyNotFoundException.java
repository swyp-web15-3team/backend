package com.team3.collection.exception;

import com.team3.common.exception.CustomException;
import com.team3.common.exception.ErrorCode;

public class WhiskyNotFoundException extends CustomException {
    public WhiskyNotFoundException() {
        super(ErrorCode.WHISKY_NOT_FOUND);
    }
}
