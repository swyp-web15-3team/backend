package com.team3.whisky.dto;

import java.util.List;

public record WhiskyCategoriesResponse(List<Category> categories) {

    public record Category(Long id, String name) {
    }
}
