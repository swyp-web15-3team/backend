package com.team3.whisky.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.team3.whisky.Whisky;
import com.team3.whisky.WhiskyCategory;
import com.team3.whisky.WhiskyLatestPrice;
import com.team3.whisky.WhiskyOrigin;
import com.team3.whisky.WhiskyRegion;

public record WhiskyListResponse(
    List<WhiskyItem> content,
    int page,
    int size,
    long totalElements,
    int totalPages) {

    public record WhiskyItem(
        Long id,
        String name,
        String imageUrl,
        Integer volumeMl,
        BigDecimal abv,
        NamedRef category,
        NamedRef origin,
        NamedRef region,
        CountryPrice kr,
        JpPrice jp,
        Comparison comparison) {

        public static WhiskyItem from(Whisky whisky, WhiskyLatestPrice kr, WhiskyLatestPrice jp) {
            return new WhiskyItem(
                whisky.id(),
                whisky.name(),
                whisky.imageUrl(),
                whisky.volumeMl(),
                whisky.abv(),
                named(whisky.category()),
                named(whisky.origin()),
                named(whisky.region()),
                krPrice(kr),
                jpPrice(jp),
                null);
        }

        private static NamedRef named(WhiskyCategory category) {
            return new NamedRef(category.id(), category.name());
        }

        private static NamedRef named(WhiskyOrigin origin) {
            if (origin == null) {
                return null;
            }
            return new NamedRef(origin.id(), origin.name());
        }

        private static NamedRef named(WhiskyRegion region) {
            if (region == null) {
                return null;
            }
            return new NamedRef(region.id(), region.name());
        }

        private static CountryPrice krPrice(WhiskyLatestPrice price) {
            if (price == null) {
                return null;
            }
            return new CountryPrice(
                price.amount(), price.currencyCode(), price.retailerName(), price.collectedAt(), false);
        }

        private static JpPrice jpPrice(WhiskyLatestPrice price) {
            if (price == null) {
                return null;
            }
            return new JpPrice(
                price.amount(),
                price.currencyCode(),
                null,
                price.retailerName(),
                price.collectedAt(),
                false);
        }
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
