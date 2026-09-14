package com.team3.whisky;

import com.team3.common.ApiResponse;
import com.team3.whisky.dto.WhiskyCategoriesResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WhiskyCategoryController {

    private final WhiskyCategoryService categories;

    public WhiskyCategoryController(WhiskyCategoryService categories) {
        this.categories = categories;
    }

    @GetMapping("/api/v1/whisky-categories")
    public ApiResponse<WhiskyCategoriesResponse> getCategories() {
        return ApiResponse.of(categories.getCategories());
    }
}
