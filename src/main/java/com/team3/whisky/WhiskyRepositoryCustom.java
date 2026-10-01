package com.team3.whisky;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WhiskyRepositoryCustom {

    List<Whisky> findRelated(Long excludedWhiskyId, Long categoryId, Long originId, int limit);

    Page<Whisky> findByCollectionId(Long collectionId, Pageable pageable);

    Page<Whisky> search(
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
        Pageable pageable);
}
