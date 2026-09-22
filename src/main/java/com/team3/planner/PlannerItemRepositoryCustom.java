package com.team3.planner;

/**
 * 플래너 항목 범위 삭제. {@code JpaRepository}에 {@code deleteBy…}를 선언하지 않는다.
 * Spring Data는 그 이름을 SELECT 후 엔티티마다 {@code remove}로 구현해서, 초기화처럼
 * 여러 행을 지울 때 DELETE가 행 수만큼 나간다. 그래서 이 Custom에서 벌크 삭제로 둔다.
 */
public interface PlannerItemRepositoryCustom {

    long deleteByPlannerId(Long plannerId);

    long deleteByPlannerIdAndListType(Long plannerId, PlannerListType listType);

    long deleteByPlannerIdAndListTypeAndSaleProductId(
        Long plannerId, PlannerListType listType, Long saleProductId);
}
