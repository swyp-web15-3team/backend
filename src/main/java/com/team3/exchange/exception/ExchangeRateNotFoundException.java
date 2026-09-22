package com.team3.exchange.exception;

import com.team3.common.exception.CustomException;
import com.team3.common.exception.ErrorCode;

public class ExchangeRateNotFoundException extends CustomException {
    public ExchangeRateNotFoundException() {
        super(ErrorCode.EXCHANGE_RATE_NOT_FOUND);
    }
}
