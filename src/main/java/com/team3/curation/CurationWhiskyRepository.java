package com.team3.curation;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CurationWhiskyRepository extends JpaRepository<CurationWhisky, Long> {

    List<CurationWhisky> findByCurationIdInOrderByIdAsc(Collection<Long> curationIds);
}
