package com.team3.whisky;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WhiskyRepository extends JpaRepository<Whisky, Long> {

    List<Whisky> findTop10ByOrderByIdAsc();

    List<Whisky> findTop10ByNameContainingOrderByIdAsc(String name);
}
