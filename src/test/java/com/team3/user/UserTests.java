package com.team3.user;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UserTests {
    @Test
    void acceptsOpaqueStringIdsAndRejectsMissingOrOversizedIdentity() {
        assertThatCode(() -> new User(Provider.KAKAO, "external:Abc-001")).doesNotThrowAnyException();
        assertThatCode(() -> new User(Provider.KAKAO, "a".repeat(255))).doesNotThrowAnyException();
        assertThatThrownBy(() -> new User(null, "id")).isInstanceOf(IllegalArgumentException.class);
        for (String id : new String[]{null, "", " \t", "a".repeat(256)}) {
            assertThatThrownBy(() -> new User(Provider.KAKAO, id)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
