package com.team3.whisky;

import java.util.Collection;
import java.util.List;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;

public class PriceHistoryRepositoryImpl implements PriceHistoryRepositoryCustom {

    private static final QPriceHistory PRICE_HISTORY = QPriceHistory.priceHistory;
    private static final QSaleProduct SALE_PRODUCT = QSaleProduct.saleProduct;
    private static final QRetailer RETAILER = QRetailer.retailer;
    private static final QPriceHistory LATEST = new QPriceHistory("latest");

    private final JPAQueryFactory queryFactory;

    public PriceHistoryRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public List<WhiskyLatestPrice> findLatestAvailablePrices(Collection<Long> whiskyIds) {
        if (whiskyIds.isEmpty()) {
            return List.of();
        }
        return queryFactory
            .select(Projections.constructor(
                WhiskyLatestPrice.class,
                SALE_PRODUCT.whisky.id,
                PRICE_HISTORY.price,
                PRICE_HISTORY.currencyCode,
                RETAILER.countryCode,
                RETAILER.name,
                PRICE_HISTORY.collectedAt,
                SALE_PRODUCT.id))
            .from(PRICE_HISTORY)
            .join(PRICE_HISTORY.saleProduct, SALE_PRODUCT)
            .join(SALE_PRODUCT.retailer, RETAILER)
            .where(
                SALE_PRODUCT.whisky.id.in(whiskyIds),
                SALE_PRODUCT.soldOut.eq(false),
                PRICE_HISTORY.collectedAt.eq(
                    JPAExpressions.select(LATEST.collectedAt.max())
                        .from(LATEST)
                        .where(LATEST.saleProduct.eq(SALE_PRODUCT))))
            .fetch();
    }

    @Override
    public List<WhiskyLatestPrice> findLatestPrices(Collection<Long> saleProductIds) {
        if (saleProductIds.isEmpty()) {
            return List.of();
        }
        return queryFactory
            .select(Projections.constructor(
                WhiskyLatestPrice.class,
                SALE_PRODUCT.whisky.id,
                PRICE_HISTORY.price,
                PRICE_HISTORY.currencyCode,
                RETAILER.countryCode,
                RETAILER.name,
                PRICE_HISTORY.collectedAt,
                SALE_PRODUCT.id))
            .from(PRICE_HISTORY)
            .join(PRICE_HISTORY.saleProduct, SALE_PRODUCT)
            .join(SALE_PRODUCT.retailer, RETAILER)
            .where(
                SALE_PRODUCT.id.in(saleProductIds),
                PRICE_HISTORY.collectedAt.eq(
                    JPAExpressions.select(LATEST.collectedAt.max())
                        .from(LATEST)
                        .where(LATEST.saleProduct.eq(SALE_PRODUCT))))
            .fetch();
    }
}
