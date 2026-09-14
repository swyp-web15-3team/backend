package com.team3.whisky;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WhiskyCategoryRepository extends JpaRepository<WhiskyCategory, Long> {
    List<WhiskyCategory> findAllByOrderByIdAsc();
}
