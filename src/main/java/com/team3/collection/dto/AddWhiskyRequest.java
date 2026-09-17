package com.team3.collection.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddWhiskyRequest(
    @NotNull(message = "위스키 ID는 필수입니다.") @Positive(message = "위스키 ID는 양수여야 합니다.") Long whiskyId) {
}
