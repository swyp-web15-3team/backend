package com.team3.collection.exception;

import com.team3.common.exception.CustomException;

public class SameCollectionCopyException extends CustomException {

    public SameCollectionCopyException() {
        super(CollectionErrorCode.COLLECTION_SAME_GROUP_COPY);
    }
}
