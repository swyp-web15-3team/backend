package com.team3.collection.exception;

import com.team3.common.exception.CustomException;

public class DuplicateCollectionNameException extends CustomException {

    public DuplicateCollectionNameException() {
        super(CollectionErrorCode.COLLECTION_NAME_DUPLICATE);
    }
}
