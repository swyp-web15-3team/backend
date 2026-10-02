package com.team3.exchange;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import com.team3.exchange.exception.ExchangeRateNotConfiguredException;
import com.team3.exchange.exception.ExchangeRateNotFoundException;
import com.team3.exchange.exception.ExchangeRateProviderException;

import org.springframework.stereotype.Component;

@Component
public class YenExchangeRateLookup {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int MAX_DAYS_BACK = 7;

    private final ExchangeRateService exchangeRates;

    public YenExchangeRateLookup(ExchangeRateService exchangeRates) {
        this.exchangeRates = exchangeRates;
    }

    public BigDecimal findKrwPerJpy() {
        LocalDate today = LocalDate.now(SEOUL);
        for (int daysBack = 0; daysBack < MAX_DAYS_BACK; daysBack++) {
            try {
                BigDecimal rate = ExchangeRateResponse.from(exchangeRates.getRates(today.minusDays(daysBack)))
                    .rates().stream()
                    .filter(item -> "JPY".equals(item.currency()))
                    .map(ExchangeRateResponse.Rate::rate)
                    .findFirst()
                    .orElse(null);
                if (rate == null || rate.signum() <= 0) {
                    return null;
                }
                return rate;
            } catch (ExchangeRateNotFoundException exception) {
                // Only a missing publication falls back to an earlier date.
                continue;
            } catch (ExchangeRateProviderException | ExchangeRateNotConfiguredException
                | IllegalArgumentException | NullPointerException exception) {
                return null;
            }
        }
        return null;
    }
}
