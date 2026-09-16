package com.team3.collection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCollectionRequest(
    @NotBlank(message = "관심 그룹 이름은 필수입니다.") @Size(max = 50, message = "관심 그룹 이름은 50자 이하여야 합니다.") String name) {

    public CreateCollectionRequest {
        name = name == null ? null : name.strip();
    }
}
