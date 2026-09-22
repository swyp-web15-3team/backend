package com.team3.planner;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PlannerItemRepository extends JpaRepository<PlannerItem, Long> {

    List<PlannerItem> findByPlannerIdOrderByIdAsc(Long plannerId);

    @Modifying
    @Query("delete from PlannerItem item where item.plannerId = ?1")
    long deleteByPlannerId(Long plannerId);

    @Modifying
    @Query("delete from PlannerItem item where item.plannerId = ?1 and item.listType = ?2")
    long deleteByPlannerIdAndListType(Long plannerId, PlannerListType listType);

    @Modifying
    @Query("delete from PlannerItem item where item.plannerId = ?1 and item.listType = ?2 and item.saleProductId = ?3")
    long deleteByPlannerIdAndListTypeAndSaleProductId(
        Long plannerId, PlannerListType listType, Long saleProductId);
}
