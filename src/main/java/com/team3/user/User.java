package com.team3.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_provider_id", columnNames = {"provider",
        "provider_id"}))
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Provider provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    protected User() {
    }

    public User(Provider provider, String providerId) {
        if (provider == null || providerId == null || providerId.isBlank() || providerId.length() > 255) {
            throw new IllegalArgumentException("Provider and a provider ID of 1 to 255 characters are required.");
        }
        this.provider = provider;
        this.providerId = providerId;
    }

    public Long id() {
        return id;
    }
}
