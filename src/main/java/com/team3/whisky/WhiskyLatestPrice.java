package com.team3.whisky;

import java.math.BigDecimal;
import java.time.Instant;

public record WhiskyLatestPrice(
    Long whiskyId,
    BigDecimal amount,
    String currencyCode,
    String countryCode,
    String retailerName,
    Instant collectedAt,
    Long saleProductId) {
}
