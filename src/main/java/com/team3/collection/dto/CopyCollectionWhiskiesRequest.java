package com.team3.collection.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CopyCollectionWhiskiesRequest(
    @NotNull(message = "도착 관심 그룹 ID는 필수입니다.") @Positive(message = "도착 관심 그룹 ID는 양수여야 합니다.") Long targetCollectionId,
    @NotEmpty(message = "위스키 ID는 하나 이상 필요합니다.") @Size(max = 20, message = "위스키 ID는 최대 20개까지 가능합니다.") List<@NotNull(message = "위스키 ID는 필수입니다.") @Positive(message = "위스키 ID는 양수여야 합니다.") Long> whiskyIds) {
}
