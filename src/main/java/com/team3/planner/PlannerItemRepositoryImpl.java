package com.team3.planner;

import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;

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
