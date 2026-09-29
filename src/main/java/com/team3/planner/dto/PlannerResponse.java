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
import com.team3.whisky.WhiskyCategory;
import com.team3.whisky.WhiskyLatestPrice;

public record PlannerResponse(List<PlannerListItemResponse> items) {

    public static PlannerResponse empty() {
        return new PlannerResponse(List.of());
    }

    public static PlannerResponse from(
        List<PlannerItem> items,
        Map<Long, SaleProduct> products,
        Map<Long, WhiskyLatestPrice> latestPrices) {
        List<PlannerListItemResponse> responses = new ArrayList<>();
        for (PlannerItem item : items) {
            responses.add(
                PlannerListItemResponse.from(
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

    public record PlannerListItemResponse(
        Long plannerItemId,
        PlannerListType listType,
        Long saleProductId,
        Long whiskyId,
        String whiskyName,
        Category category,
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

        private static PlannerListItemResponse from(
            PlannerItem item, SaleProduct product, WhiskyLatestPrice latestPrice) {
            PlannerItemResponse base = PlannerItemResponse.from(item, product, latestPrice);
            Whisky whisky = product == null ? null : product.whisky();
            WhiskyCategory category = whisky == null ? null : whisky.category();
            Category responseCategory = category == null ? null : new Category(category.id(), category.name());
            return new PlannerListItemResponse(
                base.plannerItemId(),
                base.listType(),
                base.saleProductId(),
                base.whiskyId(),
                base.whiskyName(),
                responseCategory,
                base.volumeMl(),
                base.abv(),
                base.retailerId(),
                base.retailerName(),
                base.countryCode(),
                base.isDutyFree(),
                base.productUrl(),
                base.isSoldOut(),
                base.price(),
                base.exchange(),
                base.computable());
        }
    }

    public record Category(Long id, String name) {
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
