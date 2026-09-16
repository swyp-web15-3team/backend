package com.team3.whisky;

import com.team3.common.ApiResponse;
import com.team3.whisky.dto.WhiskySuggestionsResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WhiskyController {

    private final WhiskyService whiskies;

    public WhiskyController(WhiskyService whiskies) {
        this.whiskies = whiskies;
    }

    @GetMapping("/api/v1/whiskies/suggestions")
    public ApiResponse<WhiskySuggestionsResponse> getSuggestions(@RequestParam(required = false) String query) {
        return ApiResponse.of(whiskies.getSuggestions(query));
    }
}
