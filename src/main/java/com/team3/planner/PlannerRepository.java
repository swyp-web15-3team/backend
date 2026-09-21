package com.team3.planner;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlannerRepository extends JpaRepository<Planner, Long> {

    Optional<Planner> findByUserId(Long userId);
}
