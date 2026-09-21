package com.team3.curation;

import com.team3.common.ApiResponse;
import com.team3.curation.dto.CurationListResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurationController {

    private final CurationService curations;

    public CurationController(CurationService curations) {
        this.curations = curations;
    }

    @GetMapping("/api/v1/curations")
    public ApiResponse<CurationListResponse> getCurations() {
        return ApiResponse.of(curations.getCurations());
    }
}
