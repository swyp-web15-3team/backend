package com.team3.collection.dto;

import java.util.List;

import com.team3.whisky.dto.WhiskyListResponse;
import com.team3.whisky.dto.WhiskyListResponse.WhiskyItem;

public record CollectionWhiskiesResponse(
    List<WhiskyItem> items,
    int page,
    int size,
    long totalElements,
    int totalPages) {

    public static CollectionWhiskiesResponse from(WhiskyListResponse response) {
        return new CollectionWhiskiesResponse(
            response.content(), response.page(), response.size(), response.totalElements(), response.totalPages());
    }
}
