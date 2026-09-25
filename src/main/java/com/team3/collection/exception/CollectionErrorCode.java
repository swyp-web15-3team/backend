package com.team3.collection.exception;

import com.team3.common.exception.ErrorCodeDefinition;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CollectionErrorCode implements ErrorCodeDefinition {
    COLLECTION_NAME_DUPLICATE(HttpStatus.CONFLICT, "COLLECTION_001", "이미 존재하는 관심 그룹 이름입니다."),

    COLLECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "COLLECTION_002", "관심 그룹을 찾을 수 없습니다."),

    DEFAULT_COLLECTION_IMMUTABLE(HttpStatus.BAD_REQUEST, "COLLECTION_003", "기본 관심 목록은 수정할 수 없습니다."),

    COLLECTION_REQUEST_INVALID(HttpStatus.BAD_REQUEST, "COLLECTION_004", "요청 값이 올바르지 않습니다."),

    COLLECTION_PAGE_SIZE_INVALID(HttpStatus.BAD_REQUEST, "COLLECTION_005", "size는 1 이상 50 이하여야 합니다."),

    COLLECTION_SAME_GROUP(HttpStatus.BAD_REQUEST, "COLLECTION_006", "같은 관심 그룹으로는 이동할 수 없습니다."),

    COLLECTION_SAME_GROUP_COPY(HttpStatus.BAD_REQUEST, "COLLECTION_007", "같은 관심 그룹으로는 복사할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
