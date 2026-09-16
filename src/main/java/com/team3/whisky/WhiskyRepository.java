package com.team3.whisky;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WhiskyRepository extends JpaRepository<Whisky, Long> {

    List<Whisky> findAllBy(Sort sort, Limit limit);

    List<Whisky> findByNameContaining(String name, Sort sort, Limit limit);

    @EntityGraph(attributePaths = {"category", "origin", "region"})
    @Query("""
        SELECT w FROM Whisky w
        WHERE (:keyword IS NULL OR w.name LIKE CONCAT('%', :keyword, '%'))
            AND (:categoryId IS NULL OR w.category.id = :categoryId)
            AND (:originId IS NULL OR w.origin.id = :originId)
            AND (:regionId IS NULL OR w.region.id = :regionId)
            AND (:volumeMl IS NULL OR w.volumeMl = :volumeMl)
            AND ((:countryCode IS NULL AND :isDutyFree IS NULL) OR EXISTS (
                SELECT 1
                FROM SaleProduct saleProduct
                JOIN saleProduct.retailer retailer
                WHERE saleProduct.whisky = w
                    AND (:countryCode IS NULL OR retailer.countryCode = :countryCode)
                    AND (:isDutyFree IS NULL OR retailer.dutyFree = :isDutyFree)
            ))
        """)
    Page<Whisky> search(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        @Param("originId") Long originId,
        @Param("regionId") Long regionId,
        @Param("volumeMl") Integer volumeMl,
        @Param("countryCode") String countryCode,
        @Param("isDutyFree") Boolean isDutyFree,
        Pageable pageable);
}
