package com.team3.exchange.exception;

import com.team3.common.exception.CustomException;
import com.team3.common.exception.ErrorCode;

public class ExchangeRateProviderException extends CustomException {
    public ExchangeRateProviderException() {
        super(ErrorCode.EXCHANGE_RATE_PROVIDER_ERROR);
    }
}
