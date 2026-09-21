package com.team3.planner;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlannerItemRepository extends JpaRepository<PlannerItem, Long> {

    List<PlannerItem> findByPlannerIdOrderByIdAsc(Long plannerId);

    long deleteByPlannerId(Long plannerId);

    long deleteByPlannerIdAndListType(Long plannerId, PlannerListType listType);

    long deleteByPlannerIdAndListTypeAndSaleProductId(
        Long plannerId, PlannerListType listType, Long saleProductId);
}
