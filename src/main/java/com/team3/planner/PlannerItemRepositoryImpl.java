package com.team3.planner;

import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;

/**
 * QueryDSL {@code delete().execute()}는 엔티티를 불러오지 않고 DELETE 한 번만 보낸다.
 * {@code @Modifying @Query} JPQL도 동작은 같지만, 이 프로젝트는 커스텀 쿼리를 QueryDSL로 두고 JPQL은
 * QueryDSL로 못 쓸 때만 쓴다. 이 삭제는 그 예외가 아니다.
 */
public class PlannerItemRepositoryImpl implements PlannerItemRepositoryCustom {

    private static final QPlannerItem PLANNER_ITEM = QPlannerItem.plannerItem;

    private final JPAQueryFactory queryFactory;

    public PlannerItemRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public long deleteByPlannerId(Long plannerId) {
        return queryFactory
            .delete(PLANNER_ITEM)
            .where(PLANNER_ITEM.plannerId.eq(plannerId))
            .execute();
    }

    @Override
    public long deleteByPlannerIdAndListType(Long plannerId, PlannerListType listType) {
        return queryFactory
            .delete(PLANNER_ITEM)
            .where(
                PLANNER_ITEM.plannerId.eq(plannerId),
                PLANNER_ITEM.listType.eq(listType))
            .execute();
    }

    @Override
    public long deleteByPlannerIdAndListTypeAndSaleProductId(
        Long plannerId, PlannerListType listType, Long saleProductId) {
        return queryFactory
            .delete(PLANNER_ITEM)
            .where(
                PLANNER_ITEM.plannerId.eq(plannerId),
                PLANNER_ITEM.listType.eq(listType),
                PLANNER_ITEM.saleProductId.eq(saleProductId))
            .execute();
    }
}
