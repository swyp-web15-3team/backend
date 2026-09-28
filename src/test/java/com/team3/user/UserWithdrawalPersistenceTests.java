package com.team3.user;

import com.team3.user.enums.Provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = UserWithdrawalPersistenceTests.UserJpaConfig.class)
@ActiveProfiles("test")
class UserWithdrawalPersistenceTests {

    @Autowired
    private UserRepository users;

    @Autowired
    private EntityManager entityManager;

    @Test
    void retainsWithdrawalReasonAfterClearingPersistenceContext() {
        User user = users.saveAndFlush(new User(Provider.KAKAO, "withdrawal-persistence"));
        Long userId = user.id();
        user.updateNickname("former member");
        user.delete(Instant.parse("2026-09-28T00:00:00Z"), "No longer needed");
        entityManager.flush();
        entityManager.clear();

        User reloaded = users.findById(userId).orElseThrow();
        assertThat(reloaded.withdrawalReason()).isEqualTo("No longer needed");
        assertThat(reloaded.isDeleted()).isTrue();
        assertThat(reloaded.providerId()).isNull();
        assertThat(reloaded.nickname()).isNull();
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = User.class)
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    static class UserJpaConfig {
    }
}
