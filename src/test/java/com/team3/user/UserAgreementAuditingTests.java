package com.team3.user;

import com.team3.user.enums.AgreementType;
import com.team3.user.enums.Provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import com.team3.common.JpaAuditingConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = UserAgreementAuditingTests.UserJpaConfig.class)
@ActiveProfiles("test")
class UserAgreementAuditingTests {

    @Autowired
    private UserAgreementRepository agreements;

    @Autowired
    private UserRepository users;

    @Test
    void stampsAgreedAtOnInsertWithoutAnExplicitInstant() {
        User user = users.save(new User(Provider.KAKAO, "auditing"));
        LocalDateTime before = LocalDateTime.now();
        UserAgreement saved = agreements
            .saveAndFlush(new UserAgreement(user.id(), AgreementType.TERMS_OF_SERVICE, true));
        assertThat(saved.agreedAt()).isBetween(before, LocalDateTime.now());
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = User.class)
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    @Import(JpaAuditingConfig.class)
    static class UserJpaConfig {
    }
}
