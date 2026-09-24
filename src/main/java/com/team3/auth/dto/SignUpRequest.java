package com.team3.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
    @AssertTrue(message = "만 14세 이상 동의가 필요합니다.") boolean ageOver14Agreed,
    @AssertTrue(message = "이용약관 동의가 필요합니다.") boolean termsOfServiceAgreed,
    @AssertTrue(message = "개인정보 수집 및 이용 동의가 필요합니다.") boolean privacyPolicyAgreed,
    boolean marketingAgreed,
    @NotBlank @Size(max = 30) String nickname) {
}
