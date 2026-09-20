package com.team3.curation.dto;

import java.util.List;

import com.team3.whisky.dto.WhiskyListResponse.WhiskyItem;

public record CurationListResponse(List<CurationItem> curations) {

    public record CurationItem(Long id, String title, List<WhiskyItem> whiskies) {
    }
}
