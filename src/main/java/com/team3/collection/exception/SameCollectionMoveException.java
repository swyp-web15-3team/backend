package com.team3.collection.exception;

import com.team3.common.exception.CustomException;
public class SameCollectionMoveException extends CustomException {

    public SameCollectionMoveException() {
        super(CollectionErrorCode.COLLECTION_SAME_GROUP);
    }
}
