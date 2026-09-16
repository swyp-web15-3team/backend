package com.team3.collection.dto;

import com.team3.collection.Collection;

public record CollectionResponse(Long id, String name, boolean isDefault) {

    public static CollectionResponse from(Collection collection) {
        return new CollectionResponse(collection.id(), collection.name(), collection.isDefault());
    }
}
