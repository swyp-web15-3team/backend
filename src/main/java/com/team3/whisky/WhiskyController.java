package com.team3.whisky;

import com.team3.common.ApiResponse;
import com.team3.whisky.dto.WhiskyDetailResponse;
import com.team3.whisky.dto.WhiskyListResponse;
import com.team3.whisky.dto.WhiskyRelatedResponse;
import com.team3.whisky.dto.WhiskySuggestionsResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WhiskyController {

    private final WhiskyService whiskies;

    public WhiskyController(WhiskyService whiskies) {
        this.whiskies = whiskies;
    }

    @GetMapping("/api/v1/whiskies")
    public ApiResponse<WhiskyListResponse> getWhiskies(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Long originId,
        @RequestParam(required = false) Long regionId,
        @RequestParam(required = false) Integer volumeMl,
        @RequestParam(required = false) String countryCode,
        @RequestParam(required = false) Boolean isDutyFree,
        @RequestParam(required = false) String sort,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size) {
        return ApiResponse.of(
            whiskies.getWhiskies(
                query, categoryId, originId, regionId, volumeMl, countryCode, isDutyFree, sort, page, size));
    }

    @GetMapping("/api/v1/whiskies/suggestions")
    public ApiResponse<WhiskySuggestionsResponse> getSuggestions(@RequestParam(required = false) String query) {
        return ApiResponse.of(whiskies.getSuggestions(query));
    }

    @GetMapping("/api/v1/whiskies/{whiskyId}")
    public ApiResponse<WhiskyDetailResponse> getWhisky(@PathVariable Long whiskyId) {
        return ApiResponse.of(whiskies.getWhisky(whiskyId));
    }

    @GetMapping("/api/v1/whiskies/{whiskyId}/related")
    public ApiResponse<WhiskyRelatedResponse> getRelated(@PathVariable Long whiskyId) {
        return ApiResponse.of(whiskies.getRelated(whiskyId));
    }
}
