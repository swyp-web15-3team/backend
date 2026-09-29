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
            Map<Long, WhiskyLatestPrice> latestPrices) {
            List<PlannerItemResponse> responses = new ArrayList<>();
            for (PlannerItem item : created) {
                responses.add(from(item, products.get(item.saleProductId()), latestPrices.get(item.saleProductId())));
            }
            return responses;
        }

        public static PlannerItemResponse from(PlannerItem item, SaleProduct product, WhiskyLatestPrice latestPrice) {
            Whisky whisky = product.whisky();
            Retailer retailer = product.retailer();
            Price price = toPrice(latestPrice);
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
                null,
                price != null && price.amountKrw() != null);
        }

        private static Price toPrice(WhiskyLatestPrice latestPrice) {
            if (latestPrice == null) {
                return null;
            }
            BigDecimal amountKrw = "KRW".equals(latestPrice.currencyCode()) ? latestPrice.amount() : null;
            boolean stale = latestPrice.collectedAt().isBefore(Instant.now().minus(Duration.ofHours(24)));
            return new Price(
                latestPrice.amount(),
                latestPrice.currencyCode(),
                amountKrw,
                latestPrice.collectedAt(),
                stale);
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
