package com.team3.whisky;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long>, PriceHistoryRepositoryCustom {
}
