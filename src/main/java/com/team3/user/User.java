package com.team3.user;

import com.team3.user.enums.UserStatus;

import com.team3.user.enums.Provider;

import java.time.Instant;

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

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserStatus status;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(length = 30)
    private String nickname;

    @Column(name = "profile_image_url", length = 2048)
    private String profileImageUrl;

    protected User() {
    }

    public User(Provider provider, String providerId) {
        if (provider == null || providerId == null || providerId.isBlank() || providerId.length() > 255) {
            throw new IllegalArgumentException("Provider and a provider ID of 1 to 255 characters are required.");
        }
        this.provider = provider;
        this.providerId = providerId;
        this.status = UserStatus.PENDING;
    }

    public Long id() {
        return id;
    }

    public boolean isPending() {
        return status == UserStatus.PENDING;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public String providerId() {
        return providerId;
    }

    public Instant deletedAt() {
        return deletedAt;
    }

    public String nickname() {
        return nickname;
    }

    public String profileImageUrl() {
        return profileImageUrl;
    }

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void delete(Instant deletedAt) {
        if (this.deletedAt == null) {
            this.deletedAt = deletedAt;
            this.providerId = null;
            this.nickname = null;
            this.profileImageUrl = null;
        }
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }
}
