package com.team3.exchange;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRateSnapshot, LocalDate> {
    // 1163413842: 환율 잠금용으로 임의 지정한 구분값
    @Query(value = "select 1 from pg_advisory_xact_lock(1163413842, :day)", nativeQuery = true)
    int lockDate(@Param("day") int day);
}
