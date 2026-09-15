package com.team3.collection.exception;

import com.team3.common.exception.CustomException;
import com.team3.common.exception.ErrorCode;

public class DefaultCollectionImmutableException extends CustomException {

    public DefaultCollectionImmutableException() {
        super(ErrorCode.DEFAULT_COLLECTION_IMMUTABLE);
    }
}
