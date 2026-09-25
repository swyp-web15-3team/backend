package com.team3.collection.exception;

import com.team3.common.exception.CustomException;

public class CollectionNotFoundException extends CustomException {
    public CollectionNotFoundException() {
        super(CollectionErrorCode.COLLECTION_NOT_FOUND);
    }
}
