package com.team3.collection.exception;

import com.team3.common.exception.CustomException;
import com.team3.common.exception.ErrorCode;

public class CollectionNotFoundException extends CustomException {
    public CollectionNotFoundException() {
        super(ErrorCode.COLLECTION_NOT_FOUND);
    }
}
