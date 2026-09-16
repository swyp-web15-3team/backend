package com.team3.collection.exception;

import com.team3.common.exception.CustomException;
import com.team3.common.exception.ErrorCode;

public class DuplicateCollectionNameException extends CustomException {

    public DuplicateCollectionNameException() {
        super(ErrorCode.COLLECTION_NAME_DUPLICATE);
    }
}
