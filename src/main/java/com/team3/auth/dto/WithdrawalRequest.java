package com.team3.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WithdrawalRequest(@NotBlank @Size(max = 500) String reason) {
}
