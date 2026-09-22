package com.team3.curation.dto;

import java.util.List;

import com.team3.whisky.dto.WhiskyListResponse.WhiskyItem;

public record CurationListResponse(
    Long id,
    String title,
    List<WhiskyItem> content,
    int page,
    int size,
    long totalElements,
    int totalPages) {

    public static CurationListResponse empty(int page, int size) {
        return new CurationListResponse(null, null, List.of(), page, size, 0L, 0);
    }
}
