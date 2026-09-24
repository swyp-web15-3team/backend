package com.team3.collection.exception;

import com.team3.common.exception.CustomException;
import com.team3.common.exception.ErrorCode;

public class SameCollectionMoveException extends CustomException {

    public SameCollectionMoveException() {
        super(ErrorCode.COLLECTION_SAME_GROUP);
    }
}
