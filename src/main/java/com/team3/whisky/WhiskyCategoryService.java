package com.team3.whisky;

import java.util.List;

import com.team3.whisky.dto.WhiskyCategoriesResponse;
import com.team3.whisky.dto.WhiskyCategoriesResponse.Category;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WhiskyCategoryService {

    private final WhiskyCategoryRepository categories;

    public WhiskyCategoryService(WhiskyCategoryRepository categories) {
        this.categories = categories;
    }

    public WhiskyCategoriesResponse getCategories() {
        List<Category> responses = categories.findAllByOrderByIdAsc().stream()
            .map(category -> new Category(category.id(), category.name()))
            .toList();
        return new WhiskyCategoriesResponse(responses);
    }
}
