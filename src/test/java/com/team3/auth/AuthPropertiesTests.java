package com.team3.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import com.team3.auth.jwt.JwtConfig;
import com.team3.auth.jwt.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

class AuthPropertiesTests {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfig.class, JwtConfig.class, KakaoClient.class)
        .withBean(RestClient.Builder.class, RestClient::builder)
        .withPropertyValues("auth.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            "auth.jwt.issuer=backend", "auth.jwt.access-ttl=15m", "auth.jwt.refresh-ttl=14d",
            "auth.kakao.enabled=false", "auth.kakao.client-id=", "auth.kakao.client-secret=",
            "auth.kakao.redirect-uri=");

    @Test
    void disabledKakaoStartsWithEmptySettingsAndBindsJwtDurations() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(KakaoClient.class);
            JwtProperties jwt = context.getBean(JwtProperties.class);
            assertThat(jwt.issuer()).isEqualTo("backend");
            assertThat(jwt.accessTtl()).isEqualTo(Duration.ofMinutes(15));
            assertThat(jwt.refreshTtl()).isEqualTo(Duration.ofDays(14));
            assertThat(context.getBean(KakaoProperties.class).enabled()).isFalse();
        });
    }

    @Test
    void enabledKakaoBindsSettingsAndCreatesClient() {
        runner.withPropertyValues("auth.kakao.enabled=true", "auth.kakao.client-id=app",
            "auth.kakao.redirect-uri=http://localhost:3000/auth/kakao/callback")
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(KakaoClient.class);
                KakaoProperties kakao = context.getBean(KakaoProperties.class);
                assertThat(kakao.clientId()).isEqualTo("app");
                assertThat(kakao.clientSecret()).isEmpty();
                assertThat(kakao.redirectUri()).isEqualTo("http://localhost:3000/auth/kakao/callback");
            });
    }

    @Test
    void rejectsInvalidJwtSettingsAtStartup() {
        for (String invalid : new String[]{"auth.jwt.access-ttl=0s", "auth.jwt.refresh-ttl=15m",
                "auth.jwt.access-ttl=invalid", "auth.jwt.secret=YWJj", "auth.jwt.secret=%%%"}) {
            runner.withPropertyValues(invalid).run(context -> assertThat(context).hasFailed());
        }
    }

    @Test
    void enabledKakaoRejectsMissingClientIdAndInvalidRedirect() {
        runner.withPropertyValues("auth.kakao.enabled=true",
            "auth.kakao.redirect-uri=http://localhost:3000/auth/kakao/callback")
            .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("auth.kakao.enabled=true", "auth.kakao.client-id=app",
            "auth.kakao.redirect-uri=ftp://localhost/callback")
            .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({JwtProperties.class, KakaoProperties.class})
    static class PropertiesConfig {
    }
}
