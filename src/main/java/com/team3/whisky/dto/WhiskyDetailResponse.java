package com.team3.whisky.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.team3.whisky.SaleProduct;
import com.team3.whisky.Whisky;
import com.team3.whisky.WhiskyLatestPrice;
import com.team3.whisky.dto.WhiskyListResponse.Comparison;
import com.team3.whisky.dto.WhiskyListResponse.CountryPrice;
import com.team3.whisky.dto.WhiskyListResponse.JpPrice;
import com.team3.whisky.dto.WhiskyListResponse.NamedRef;
import com.team3.whisky.dto.WhiskyListResponse.WhiskyItem;

public record WhiskyDetailResponse(
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
    Comparison comparison,
    List<SaleProductItem> saleProducts) {

    public static WhiskyDetailResponse from(
        Whisky whisky,
        WhiskyLatestPrice kr,
        WhiskyLatestPrice jp,
        List<SaleProductItem> saleProducts,
        BigDecimal krwPerJpy) {
        WhiskyItem item = WhiskyItem.from(whisky, kr, jp, krwPerJpy);
        return new WhiskyDetailResponse(
            item.id(),
            item.name(),
            item.imageUrl(),
            item.volumeMl(),
            item.abv(),
            item.category(),
            item.origin(),
            item.region(),
            item.kr(),
            item.jp(),
            item.comparison(),
            saleProducts);
    }

    public record SaleProductItem(
        Long id,
        String retailerName,
        String retailerAddress,
        String countryCode,
        boolean isDutyFree,
        String productUrl,
        String imageUrl,
        Boolean isSoldOut,
        SalePrice price) {

        public static SaleProductItem from(
            SaleProduct saleProduct, WhiskyLatestPrice latestPrice, BigDecimal krwPerJpy) {
            return new SaleProductItem(
                saleProduct.id(),
                saleProduct.retailer().name(),
                saleProduct.retailer().address(),
                saleProduct.retailer().countryCode(),
                saleProduct.retailer().isDutyFree(),
                saleProduct.productUrl(),
                saleProduct.imageUrl(),
                saleProduct.isSoldOut(),
                SalePrice.from(latestPrice, krwPerJpy));
        }
    }

    public record SalePrice(
        BigDecimal amount,
        String currency,
        BigDecimal amountKrw,
        Instant collectedAt,
        boolean stale) {

        private static SalePrice from(WhiskyLatestPrice latestPrice, BigDecimal krwPerJpy) {
            if (latestPrice == null) {
                return null;
            }
            BigDecimal amountKrw = null;
            if ("KRW".equals(latestPrice.currencyCode())) {
                amountKrw = latestPrice.amount();
            } else if ("JPY".equals(latestPrice.currencyCode()) && krwPerJpy != null) {
                amountKrw = latestPrice.amount().multiply(krwPerJpy);
            }
            return new SalePrice(
                latestPrice.amount(),
                latestPrice.currencyCode(),
                amountKrw,
                latestPrice.collectedAt(),
                false);
        }
    }
}
