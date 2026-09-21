package com.team3.planner.dto;

import java.util.List;

public record AddPlannerItemsRequest(List<Item> items) {

    public record Item(Long saleProductId, Integer quantity, String listType) {
    }
}
