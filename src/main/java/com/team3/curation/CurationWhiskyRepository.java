package com.team3.curation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurationWhiskyRepository extends JpaRepository<CurationWhisky, Long> {

    Page<CurationWhisky> findByCurationIdOrderByIdAsc(Long curationId, Pageable pageable);
}
