package com.team3.collection.exception;

import com.team3.common.exception.CustomException;

public class DefaultCollectionImmutableException extends CustomException {

    public DefaultCollectionImmutableException() {
        super(CollectionErrorCode.DEFAULT_COLLECTION_IMMUTABLE);
    }
}
