package com.team3.planner;

import com.team3.common.ApiResponse;
import com.team3.planner.dto.AddPlannerItemsRequest;
import com.team3.planner.dto.AddPlannerItemsResponse;
import com.team3.planner.dto.PlannerResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/planners")
public class PlannerController {

    private final PlannerService planners;

    public PlannerController(PlannerService planners) {
        this.planners = planners;
    }

    @GetMapping
    public ApiResponse<PlannerResponse> getPlanner(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.of(planners.getPlanner(Long.valueOf(jwt.getSubject())));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AddPlannerItemsResponse> addItems(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody AddPlannerItemsRequest request) {
        return ApiResponse.of(planners.addItems(Long.valueOf(jwt.getSubject()), request));
    }

    @DeleteMapping("/items")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItems(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) String listType,
        @RequestParam(required = false) Long saleProductId) {
        planners.deleteItems(Long.valueOf(jwt.getSubject()), listType, saleProductId);
    }
}
