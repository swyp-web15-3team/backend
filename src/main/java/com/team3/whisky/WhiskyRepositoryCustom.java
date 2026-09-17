package com.team3.whisky;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WhiskyRepositoryCustom {

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
