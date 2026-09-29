package com.team3.planner.dto;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.team3.planner.PlannerItem;
import com.team3.planner.PlannerListType;
import com.team3.whisky.Retailer;
import com.team3.whisky.SaleProduct;
import com.team3.whisky.Whisky;
import com.team3.whisky.WhiskyLatestPrice;

public record AddPlannerItemsResponse(List<PlannerItemResponse> items) {

    public record PlannerItemResponse(
        Long plannerItemId,
        PlannerListType listType,
        Long saleProductId,
        Long whiskyId,
        String whiskyName,
        Integer volumeMl,
        BigDecimal abv,
        Long retailerId,
        String retailerName,
        String countryCode,
        boolean isDutyFree,
        String productUrl,
        Boolean isSoldOut,
        Price price,
        Exchange exchange,
        boolean computable) {

        public static List<PlannerItemResponse> from(List<PlannerItem> created, Map<Long, SaleProduct> products,
            Map<Long, WhiskyLatestPrice> latestPrices, BigDecimal krwPerJpy, LocalDate rateDate) {
            List<PlannerItemResponse> responses = new ArrayList<>();
            for (PlannerItem item : created) {
                responses.add(
                    from(
                        item,
                        products.get(item.saleProductId()),
                        latestPrices.get(item.saleProductId()),
                        krwPerJpy,
                        rateDate));
            }
            return responses;
        }

        public static PlannerItemResponse from(
            PlannerItem item,
            SaleProduct product,
            WhiskyLatestPrice latestPrice,
            BigDecimal krwPerJpy,
            LocalDate rateDate) {
            Whisky whisky = product.whisky();
            Retailer retailer = product.retailer();
            Price price = toPrice(latestPrice, krwPerJpy);
            Exchange exchange = toExchange(latestPrice, krwPerJpy, rateDate);
            return new PlannerItemResponse(
                item.id(),
                item.listType(),
                product.id(),
                whisky.id(),
                whisky.name(),
                whisky.volumeMl(),
                whisky.abv(),
                retailer.id(),
                retailer.name(),
                retailer.countryCode(),
                retailer.isDutyFree(),
                product.productUrl(),
                product.isSoldOut(),
                price,
                exchange,
                price != null && price.amountKrw() != null);
        }

        private static Price toPrice(WhiskyLatestPrice latestPrice, BigDecimal krwPerJpy) {
            if (latestPrice == null) {
                return null;
            }
            BigDecimal amountKrw = null;
            if ("KRW".equals(latestPrice.currencyCode())) {
                amountKrw = latestPrice.amount();
            } else if ("JPY".equals(latestPrice.currencyCode()) && krwPerJpy != null) {
                amountKrw = latestPrice.amount().multiply(krwPerJpy);
            }
            boolean stale = latestPrice.collectedAt().isBefore(Instant.now().minus(Duration.ofHours(24)));
            return new Price(
                latestPrice.amount(),
                latestPrice.currencyCode(),
                amountKrw,
                latestPrice.collectedAt(),
                stale);
        }

        private static Exchange toExchange(
            WhiskyLatestPrice latestPrice, BigDecimal krwPerJpy, LocalDate rateDate) {
            if (latestPrice == null || !"JPY".equals(latestPrice.currencyCode())
                || krwPerJpy == null || rateDate == null) {
                return null;
            }
            return new Exchange("KOREA_EXIM", krwPerJpy, rateDate, rateDate);
        }
    }

    public record Price(
        BigDecimal amount,
        String currency,
        BigDecimal amountKrw,
        Instant collectedAt,
        boolean stale) {
    }

    public record Exchange(
        String source,
        BigDecimal krwPerJpy,
        LocalDate validFrom,
        LocalDate validTo) {
    }
}
