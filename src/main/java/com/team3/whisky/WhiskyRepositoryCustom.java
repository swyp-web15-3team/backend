package com.team3.whisky;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WhiskyRepositoryCustom {

    List<Whisky> findRelated(Long excludedWhiskyId, Long categoryId, Long originId, int limit);

    Page<Whisky> findByCollectionId(Long collectionId, Pageable pageable);

    Page<Whisky> search(
        String keyword,
        Long categoryId,
        Long originId,
        Long regionId,
        Integer volumeMl,
        String countryCode,
        Boolean isDutyFree,
        Pageable pageable);
}
