package com.team3.whisky;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    @Query("""
        SELECT new com.team3.whisky.WhiskyLatestPrice(
            sp.whisky.id,
            ph.price,
            ph.currencyCode,
            retailer.countryCode,
            retailer.name,
            ph.collectedAt,
            sp.id)
        FROM PriceHistory ph
        JOIN ph.saleProduct sp
        JOIN sp.retailer retailer
        WHERE sp.whisky.id IN :whiskyIds
            AND sp.soldOut = FALSE
            AND ph.collectedAt = (
                SELECT MAX(latest.collectedAt)
                FROM PriceHistory latest
                WHERE latest.saleProduct = sp
            )
        """)
    List<WhiskyLatestPrice> findLatestAvailablePrices(@Param("whiskyIds") Collection<Long> whiskyIds);
}
