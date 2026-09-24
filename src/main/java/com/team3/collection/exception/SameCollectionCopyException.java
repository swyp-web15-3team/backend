package com.team3.collection.exception;

import com.team3.common.exception.CustomException;
import com.team3.common.exception.ErrorCode;

public class SameCollectionCopyException extends CustomException {

    public SameCollectionCopyException() {
        super(ErrorCode.COLLECTION_SAME_GROUP_COPY);
    }
}
