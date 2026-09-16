package com.team3.collection;

import com.team3.collection.dto.CollectionResponse;
import com.team3.collection.dto.CollectionsResponse;
import com.team3.collection.dto.CreateCollectionRequest;
import com.team3.collection.dto.UpdateCollectionRequest;
import com.team3.common.ApiResponse;

import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
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
}
