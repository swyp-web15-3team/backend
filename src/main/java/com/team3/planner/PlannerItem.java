package com.team3.planner;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "planner_items")
public class PlannerItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "planner_id", nullable = false)
    private Long plannerId;

    @Column(name = "sale_product_id", nullable = false)
    private Long saleProductId;

    @Enumerated(EnumType.STRING)
    @Column(name = "list_type", nullable = false, length = 16)
    private PlannerListType listType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PlannerItem() {
    }

    public PlannerItem(Long plannerId, Long saleProductId, PlannerListType listType) {
        if (plannerId == null) {
            throw new IllegalArgumentException("플래너 ID는 필수입니다.");
        }
        if (saleProductId == null) {
            throw new IllegalArgumentException("판매 상품 ID는 필수입니다.");
        }
        if (listType == null) {
            throw new IllegalArgumentException("listType은 PURCHASE 또는 CANDIDATE여야 합니다.");
        }
        this.plannerId = plannerId;
        this.saleProductId = saleProductId;
        this.listType = listType;
    }

    public Long id() {
        return id;
    }

    public Long plannerId() {
        return plannerId;
    }

    public Long saleProductId() {
        return saleProductId;
    }

    public PlannerListType listType() {
        return listType;
    }
}
