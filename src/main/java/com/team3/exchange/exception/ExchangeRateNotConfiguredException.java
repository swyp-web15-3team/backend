package com.team3.exchange.exception;

import com.team3.common.exception.CustomException;
import com.team3.common.exception.ErrorCode;

public class ExchangeRateNotConfiguredException extends CustomException {
    public ExchangeRateNotConfiguredException() {
        super(ErrorCode.EXCHANGE_RATE_NOT_CONFIGURED);
    }
}
