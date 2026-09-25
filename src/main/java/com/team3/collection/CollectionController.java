package com.team3.collection;

import com.team3.collection.dto.CollectionResponse;
import com.team3.collection.dto.CollectionWhiskiesResponse;
import com.team3.collection.dto.CollectionsResponse;
import com.team3.collection.dto.CreateCollectionRequest;
import com.team3.collection.dto.AddWhiskyRequest;
import com.team3.collection.dto.CopyCollectionWhiskiesRequest;
import com.team3.collection.dto.DeleteWhiskiesRequest;
import com.team3.collection.dto.MoveCollectionWhiskiesRequest;
import com.team3.collection.dto.UpdateCollectionRequest;
import com.team3.common.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/collections")
public class CollectionController {

    private final CollectionService collections;

    public CollectionController(CollectionService collections) {
        this.collections = collections;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CollectionResponse> createCollection(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CreateCollectionRequest request) {
        CollectionResponse response = collections.createCollection(Long.valueOf(jwt.getSubject()), request.name());
        return ApiResponse.of(response);
    }

    @GetMapping
    public ApiResponse<CollectionsResponse> getCollections(
        @AuthenticationPrincipal Jwt jwt,
        @SortDefault(sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Sort sort) {
        return ApiResponse.of(collections.getCollections(Long.valueOf(jwt.getSubject()), sort));
    }

    @GetMapping("/{collectionId}/whiskies")
    public ApiResponse<CollectionWhiskiesResponse> getCollectionWhiskies(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable @Positive Long collectionId,
        @RequestParam(defaultValue = "0") @PositiveOrZero int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ApiResponse.of(
            collections.getCollectionWhiskies(
                Long.valueOf(jwt.getSubject()), collectionId, page, size));
    }

    @PatchMapping("/{collectionId}")
    public ApiResponse<CollectionResponse> updateCollection(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long collectionId,
        @Valid @RequestBody UpdateCollectionRequest request) {
        return ApiResponse.of(
            collections.updateCollection(Long.valueOf(jwt.getSubject()), collectionId, request.name()));
    }

    @DeleteMapping("/{collectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCollection(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long collectionId) {
        collections.deleteCollection(Long.valueOf(jwt.getSubject()), collectionId);
    }

    @PostMapping("/{collectionId}/whiskies")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addWhisky(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long collectionId,
        @Valid @RequestBody AddWhiskyRequest request) {
        collections.addWhisky(Long.valueOf(jwt.getSubject()), collectionId, request.whiskyId());
    }

    @PostMapping("/{collectionId}/whiskies/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveWhiskies(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long collectionId,
        @Valid @RequestBody MoveCollectionWhiskiesRequest request) {
        collections.moveWhiskies(
            Long.valueOf(jwt.getSubject()),
            collectionId,
            request.targetCollectionId(),
            request.whiskyIds());
    }

    @PostMapping("/{collectionId}/whiskies/copy")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void copyWhiskies(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long collectionId,
        @Valid @RequestBody CopyCollectionWhiskiesRequest request) {
        collections.copyWhiskies(
            Long.valueOf(jwt.getSubject()),
            collectionId,
            request.targetCollectionId(),
            request.whiskyIds());
    }

    @DeleteMapping("/{collectionId}/whiskies")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeWhiskies(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long collectionId,
        @Valid @ModelAttribute DeleteWhiskiesRequest request) {
        collections.removeWhiskies(Long.valueOf(jwt.getSubject()), collectionId, request.whiskyIds());
    }
}
