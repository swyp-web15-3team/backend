package com.team3.whisky.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record WhiskyListResponse(
    List<WhiskyCard> content,
    int page,
    int size,
    long totalElements,
    int totalPages) {

    public record WhiskyCard(
        Long id,
        String name,
        Integer volumeMl,
        BigDecimal abv,
        NamedRef category,
        NamedRef origin,
        NamedRef region,
        CountryPrice kr,
        JpPrice jp,
        Comparison comparison) {
    }

    public record NamedRef(Long id, String name) {
    }

    public record CountryPrice(
        BigDecimal amount,
        String currency,
        String retailerName,
        Instant collectedAt,
        boolean stale) {
    }

    public record JpPrice(
        BigDecimal amount,
        String currency,
        BigDecimal amountKrw,
        String retailerName,
        Instant collectedAt,
        boolean stale) {
    }

    public record Comparison(BigDecimal diffAmountKrw, BigDecimal diffRatio, String cheaperCountry) {
    }
}
