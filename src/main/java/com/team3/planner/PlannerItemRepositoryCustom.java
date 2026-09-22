package com.team3.planner;

public interface PlannerItemRepositoryCustom {

    long deleteByPlannerId(Long plannerId);

    long deleteByPlannerIdAndListType(Long plannerId, PlannerListType listType);

    long deleteByPlannerIdAndListTypeAndSaleProductId(
        Long plannerId, PlannerListType listType, Long saleProductId);
}
