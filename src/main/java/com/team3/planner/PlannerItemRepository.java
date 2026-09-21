package com.team3.planner;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlannerItemRepository extends JpaRepository<PlannerItem, Long> {

    List<PlannerItem> findByPlannerIdOrderByIdAsc(Long plannerId);
}
