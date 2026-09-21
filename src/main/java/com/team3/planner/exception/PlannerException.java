package com.team3.planner.exception;

import java.util.Map;

import com.team3.common.exception.CustomException;
import com.team3.common.exception.ErrorCode;

public class PlannerException extends CustomException {

    public PlannerException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PlannerException(ErrorCode errorCode, Long saleProductId) {
        super(errorCode, Map.of("saleProductId", saleProductId));
    }
}
