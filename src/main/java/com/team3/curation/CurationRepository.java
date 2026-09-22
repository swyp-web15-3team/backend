package com.team3.curation;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CurationRepository extends JpaRepository<Curation, Long> {

    Optional<Curation> findFirstByOrderByIdAsc();
}
