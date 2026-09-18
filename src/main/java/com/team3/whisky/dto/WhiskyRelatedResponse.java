package com.team3.whisky.dto;

import java.util.List;

import com.team3.whisky.dto.WhiskyListResponse.WhiskyItem;

public record WhiskyRelatedResponse(List<WhiskyItem> whiskies) {
}
