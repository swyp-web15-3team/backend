package com.team3.whisky;

import java.math.BigDecimal;
import java.util.List;

import com.team3.collection.QCollectionWhisky;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.Querydsl;

public class WhiskyRepositoryImpl implements WhiskyRepositoryCustom {

    private static final QWhisky WHISKY = QWhisky.whisky;
    private static final QWhiskyCategory CATEGORY = QWhiskyCategory.whiskyCategory;
    private static final QWhiskyOrigin ORIGIN = QWhiskyOrigin.whiskyOrigin;
    private static final QWhiskyRegion REGION = QWhiskyRegion.whiskyRegion;
    private static final QSaleProduct SALE_PRODUCT = QSaleProduct.saleProduct;
    private static final QRetailer RETAILER = QRetailer.retailer;
    private static final QCollectionWhisky COLLECTION_WHISKY = QCollectionWhisky.collectionWhisky;

    private final JPAQueryFactory queryFactory;
    private final Querydsl querydsl;

    public WhiskyRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
        this.querydsl = new Querydsl(entityManager, new PathBuilder<>(Whisky.class, WHISKY.getMetadata()));
    }

    @Override
    public List<Whisky> findRelated(Long excludedWhiskyId, Long categoryId, Long originId, int limit) {
        if (categoryId == null && originId == null) {
            return List.of();
        }
        BooleanBuilder where = new BooleanBuilder();
        where.and(WHISKY.id.ne(excludedWhiskyId));
        if (categoryId != null) {
            where.and(WHISKY.category.id.eq(categoryId));
        }
        if (originId != null) {
            where.and(WHISKY.origin.id.eq(originId));
        }
        return queryFactory.selectFrom(WHISKY)
            .join(WHISKY.category, CATEGORY)
            .fetchJoin()
            .leftJoin(WHISKY.origin, ORIGIN)
            .fetchJoin()
            .leftJoin(WHISKY.region, REGION)
            .fetchJoin()
            .where(where)
            .orderBy(WHISKY.id.asc())
            .limit(limit)
            .fetch();
    }

    @Override
    public Page<Whisky> findByCollectionId(Long collectionId, Pageable pageable) {
        Long total = queryFactory.select(COLLECTION_WHISKY.id.count())
            .from(COLLECTION_WHISKY)
            .where(COLLECTION_WHISKY.collectionId.eq(collectionId))
            .fetchOne();
        long totalCount = total == null ? 0L : total;
        if (totalCount == 0L || pageable.isPaged() && pageable.getOffset() >= totalCount) {
            return new PageImpl<>(List.of(), pageable, totalCount);
        }
        List<Whisky> content = queryFactory.selectFrom(WHISKY)
            .join(COLLECTION_WHISKY)
            .on(COLLECTION_WHISKY.whiskyId.eq(WHISKY.id))
            .join(WHISKY.category, CATEGORY)
            .fetchJoin()
            .leftJoin(WHISKY.origin, ORIGIN)
            .fetchJoin()
            .leftJoin(WHISKY.region, REGION)
            .fetchJoin()
            .where(COLLECTION_WHISKY.collectionId.eq(collectionId))
            .orderBy(WHISKY.name.asc(), WHISKY.id.asc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
        return new PageImpl<>(content, pageable, totalCount);
    }

    @Override
    public Page<Whisky> search(
        String keyword,
        List<Long> categoryIds,
        Long originId,
        Long regionId,
        Integer volumeMl,
        String countryCode,
        Boolean isDutyFree,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal minPriceDiffPercent,
        BigDecimal maxPriceDiffPercent,
        BigDecimal krwPerJpy,
        Pageable pageable) {
        BooleanBuilder where = searchPredicate(keyword, categoryIds, originId, regionId, volumeMl, countryCode,
            isDutyFree, minPrice, maxPrice, minPriceDiffPercent, maxPriceDiffPercent, krwPerJpy);
        Long total = queryFactory.select(WHISKY.id.count()).from(WHISKY).where(where).fetchOne();
        long totalCount = total == null ? 0L : total;
        if (totalCount == 0L || pageable.isPaged() && pageable.getOffset() >= totalCount) {
            return new PageImpl<>(List.of(), pageable, totalCount);
        }
        JPAQuery<Whisky> contentQuery = queryFactory.selectFrom(WHISKY)
            .join(WHISKY.category, CATEGORY)
            .fetchJoin()
            .leftJoin(WHISKY.origin, ORIGIN)
            .fetchJoin()
            .leftJoin(WHISKY.region, REGION)
            .fetchJoin()
            .where(where);
        querydsl.applySorting(pageable.getSort(), contentQuery);
        List<Whisky> content = contentQuery.offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();
        return new PageImpl<>(content, pageable, totalCount);
    }

    private static BooleanBuilder searchPredicate(
        String keyword,
        List<Long> categoryIds,
        Long originId,
        Long regionId,
        Integer volumeMl,
        String countryCode,
        Boolean isDutyFree,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal minPriceDiffPercent,
        BigDecimal maxPriceDiffPercent,
        BigDecimal krwPerJpy) {
        BooleanBuilder where = new BooleanBuilder();
        if (keyword != null) {
            where.and(WHISKY.name.like("%" + keyword + "%"));
        }
        if (categoryIds != null && !categoryIds.isEmpty()) {
            where.and(WHISKY.category.id.in(categoryIds));
        }
        if (originId != null) {
            where.and(WHISKY.origin.id.eq(originId));
        }
        if (regionId != null) {
            where.and(WHISKY.region.id.eq(regionId));
        }
        if (volumeMl != null) {
            where.and(WHISKY.volumeMl.eq(volumeMl));
        }
        if (countryCode != null || isDutyFree != null) {
            where.and(JPAExpressions.selectOne()
                .from(SALE_PRODUCT)
                .join(SALE_PRODUCT.retailer, RETAILER)
                .where(
                    SALE_PRODUCT.whisky.eq(WHISKY),
                    countryCode == null ? null : RETAILER.countryCode.eq(countryCode),
                    isDutyFree == null ? null : RETAILER.dutyFree.eq(isDutyFree))
                .exists());
        }
        if (minPrice != null || maxPrice != null) {
            NumberExpression<BigDecimal> krPrice = latestCountryPrice("KR", "KRW", "kr");
            NumberExpression<BigDecimal> jpPrice = latestCountryPrice("JP", "JPY", "jp")
                .multiply(krwPerJpy);
            NumberExpression<BigDecimal> cheaperPrice = Expressions.numberTemplate(BigDecimal.class,
                "case when {0} is null then {1} when {1} is null then {0} "
                    + "when {0} <= {1} then {0} else {1} end",
                krPrice, jpPrice);
            where.and(cheaperPrice.isNotNull());
            if (minPrice != null) {
                where.and(cheaperPrice.goe(minPrice));
            }
            if (maxPrice != null) {
                where.and(cheaperPrice.loe(maxPrice));
            }
        }
        if (minPriceDiffPercent != null || maxPriceDiffPercent != null) {
            NumberExpression<BigDecimal> krPrice = latestCountryPrice("KR", "KRW", "krDiscount");
            NumberExpression<BigDecimal> jpPrice = latestCountryPrice("JP", "JPY", "jpDiscount")
                .multiply(krwPerJpy);
            NumberExpression<BigDecimal> differenceTimes100 = krPrice.subtract(jpPrice)
                .multiply(BigDecimal.valueOf(100));
            where.and(krPrice.isNotNull()).and(jpPrice.isNotNull()).and(jpPrice.lt(krPrice));
            if (minPriceDiffPercent != null) {
                where.and(differenceTimes100.goe(krPrice.multiply(minPriceDiffPercent)));
            }
            if (maxPriceDiffPercent != null) {
                where.and(differenceTimes100.lt(krPrice.multiply(maxPriceDiffPercent)));
            }
        }
        return where;
    }

    private static NumberExpression<BigDecimal> latestCountryPrice(String country, String currency, String alias) {
        QSaleProduct product = new QSaleProduct(alias + "Product");
        QRetailer retailer = new QRetailer(alias + "Retailer");
        QPriceHistory history = new QPriceHistory(alias + "History");
        QPriceHistory latest = new QPriceHistory(alias + "Latest");
        return Expressions.numberTemplate(BigDecimal.class, "({0})", JPAExpressions.select(history.price.min())
            .from(history)
            .join(history.saleProduct, product)
            .join(product.retailer, retailer)
            .where(
                product.whisky.eq(WHISKY),
                product.soldOut.eq(false),
                retailer.countryCode.eq(country),
                history.currencyCode.eq(currency),
                history.collectedAt.eq(JPAExpressions.select(latest.collectedAt.max())
                    .from(latest)
                    .where(latest.saleProduct.eq(product)))));
    }
}
