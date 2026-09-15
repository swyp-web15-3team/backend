package com.team3.auth.dto;

import jakarta.validation.constraints.AssertTrue;

public record SignUpRequest(
    @AssertTrue(message = "이용약관 동의가 필요합니다.") boolean termsOfServiceAgreed,
    @AssertTrue(message = "개인정보 처리방침 동의가 필요합니다.") boolean privacyPolicyAgreed,
    boolean marketingAgreed) {
}
