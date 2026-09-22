package com.team3.planner;

/**
 * 플래너 항목 범위 삭제·리스트 이동. {@code JpaRepository}에 {@code deleteBy…}/{@code update…}를
 * 선언하지 않는다. Spring Data는 그 이름을 SELECT 후 엔티티마다 처리해서, 여러 행을 다룰 때
 * 문이 행 수만큼 나간다. 그래서 이 Custom에서 벌크로 둔다.
 */
public interface PlannerItemRepositoryCustom {

    long deleteByPlannerId(Long plannerId);

    long deleteByPlannerIdAndListType(Long plannerId, PlannerListType listType);

    long deleteByPlannerIdAndListTypeAndSaleProductId(
        Long plannerId, PlannerListType listType, Long saleProductId);

    long updateListTypeByPlannerIdAndListType(
        Long plannerId, PlannerListType fromListType, PlannerListType toListType);

    long updateListTypeByPlannerIdAndListTypeAndSaleProductId(
        Long plannerId,
        PlannerListType fromListType,
        PlannerListType toListType,
        Long saleProductId);
}
