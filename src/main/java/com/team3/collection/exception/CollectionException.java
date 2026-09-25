package com.team3.collection.exception;

import com.team3.common.exception.CustomException;

public class CollectionException extends CustomException {

    public CollectionException(CollectionErrorCode errorCode) {
        super(errorCode);
    }
}
