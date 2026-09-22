package com.team3.planner.dto;

import java.math.BigDecimal;
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

public record PlannerResponse(List<PlannerItemResponse> items) {

    public static PlannerResponse empty() {
        return new PlannerResponse(List.of());
    }

    public static PlannerResponse from(
        List<PlannerItem> items,
        Map<Long, SaleProduct> products,
        Map<Long, WhiskyLatestPrice> latestPrices) {
        List<PlannerItemResponse> responses = new ArrayList<>();
        for (PlannerItem item : items) {
            responses.add(
                PlannerItemResponse.from(
                    item, products.get(item.saleProductId()), latestPrices.get(item.saleProductId())));
        }
        return new PlannerResponse(responses);
    }

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

        public static PlannerItemResponse from(
            PlannerItem item, SaleProduct product, WhiskyLatestPrice latestPrice) {
            Price price = toPrice(latestPrice);
            if (product == null) {
                return new PlannerItemResponse(
                    item.id(),
                    item.listType(),
                    item.saleProductId(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    null,
                    null,
                    price,
                    null,
                    false);
            }
            Whisky whisky = product.whisky();
            Retailer retailer = product.retailer();
            Boolean soldOut = product.isSoldOut();
            return new PlannerItemResponse(
                item.id(),
                item.listType(),
                product.id(),
                whisky == null ? null : whisky.id(),
                whisky == null ? null : whisky.name(),
                whisky == null ? null : whisky.volumeMl(),
                whisky == null ? null : whisky.abv(),
                retailer.id(),
                retailer.name(),
                retailer.countryCode(),
                retailer.isDutyFree(),
                product.productUrl(),
                soldOut,
                price,
                null,
                computable(soldOut, price));
        }

        private static Price toPrice(WhiskyLatestPrice latestPrice) {
            if (latestPrice == null) {
                return null;
            }
            return new Price(latestPrice.amount(), latestPrice.currencyCode(), null, latestPrice.collectedAt(), false);
        }

        private static boolean computable(Boolean soldOut, Price price) {
            return Boolean.FALSE.equals(soldOut) && price != null && price.amountKrw() != null;
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
