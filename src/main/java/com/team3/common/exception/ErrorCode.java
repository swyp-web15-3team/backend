package com.team3.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),

    COLLECTION_NAME_DUPLICATE(HttpStatus.CONFLICT, "COLLECTION_001", "이미 존재하는 관심 그룹 이름입니다."),

    COLLECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "COLLECTION_002", "관심 그룹을 찾을 수 없습니다."),

    DEFAULT_COLLECTION_IMMUTABLE(HttpStatus.BAD_REQUEST, "COLLECTION_003", "기본 관심 그룹은 수정할 수 없습니다."),

    WHISKY_NOT_FOUND(HttpStatus.NOT_FOUND, "WHISKY_001", "위스키를 찾을 수 없습니다."),

    ALREADY_SIGNED_UP(HttpStatus.CONFLICT, "AUTH_002", "이미 가입이 완료된 사용자입니다."),

    DELETED_USER(HttpStatus.FORBIDDEN, "AUTH_003", "탈퇴한 사용자입니다."),

    INVALID_USER_ID(HttpStatus.BAD_REQUEST, "AUTH_004", "유효하지 않은 사용자 ID입니다."),

    ACTIVE_USER_REQUIRED(HttpStatus.FORBIDDEN, "AUTH_005", "가입이 완료된 활성 사용자만 접근할 수 있습니다."),

    PLANNER_ITEMS_MISSING(HttpStatus.BAD_REQUEST, "PLANNER_001", "추가할 상품이 없습니다."),

    PLANNER_ITEMS_LIMIT(HttpStatus.BAD_REQUEST, "PLANNER_002", "한 번에 추가할 수 있는 상품은 20개까지입니다."),

    PLANNER_SALE_PRODUCT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "PLANNER_003", "판매 상품 ID는 필수입니다."),

    PLANNER_DUPLICATE_SALE_PRODUCT(HttpStatus.BAD_REQUEST, "PLANNER_004", "같은 판매 상품을 한 요청에 중복으로 넣을 수 없습니다."),

    PLANNER_INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "PLANNER_005", "수량은 1 이상 20 이하여야 합니다."),

    PLANNER_INVALID_LIST_TYPE(HttpStatus.BAD_REQUEST, "PLANNER_006", "listType은 PURCHASE 또는 CANDIDATE여야 합니다."),

    PLANNER_NOT_JAPANESE(HttpStatus.BAD_REQUEST, "PLANNER_007", "일본 판매 상품만 추가할 수 있습니다."),

    PLANNER_SOLD_OUT(HttpStatus.BAD_REQUEST, "PLANNER_008", "품절 상품은 추가할 수 없습니다."),

    PLANNER_STOCK_UNKNOWN(HttpStatus.BAD_REQUEST, "PLANNER_009", "구매 가능 여부가 확인되지 않은 상품입니다."),

    PLANNER_PRICE_MISSING(HttpStatus.BAD_REQUEST, "PLANNER_010", "가격 정보가 없는 상품입니다."),

    PLANNER_LIST_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "PLANNER_011", "listType이 필요합니다."),

    PLANNER_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "PLANNER_012", "플래너 항목을 찾을 수 없습니다."),

    PLANNER_ITEM_FORBIDDEN(HttpStatus.FORBIDDEN, "PLANNER_013", "접근할 수 없는 플래너 항목입니다."),

    PLANNER_SAME_LIST_TYPE(HttpStatus.BAD_REQUEST, "PLANNER_014", "같은 리스트로는 이동할 수 없습니다."),

    SALE_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "SALE_PRODUCT_001", "판매 상품을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
