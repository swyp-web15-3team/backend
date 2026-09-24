package com.team3.exchange.exception;

import com.team3.common.exception.CustomException;
import com.team3.common.exception.ErrorCode;

public class InvalidExchangeRateDateException extends CustomException {
    public InvalidExchangeRateDateException() {
        super(ErrorCode.EXCHANGE_RATE_DATE_INVALID);
    }
}
