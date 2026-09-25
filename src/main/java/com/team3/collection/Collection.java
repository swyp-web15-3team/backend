package com.team3.collection;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "collections", uniqueConstraints = @UniqueConstraint(name = "uk_collections_user_name", columnNames = {
        "user_id", "name"}))
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Collection() {
    }

    public Collection(Long userId, String name) {
        this(userId, name, false);
    }

    public Collection(Long userId, String name, boolean isDefault) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        String normalizedName = name == null ? null : name.strip();
        if (normalizedName == null || normalizedName.isEmpty() || normalizedName.length() > 50) {
            throw new IllegalArgumentException("관심 그룹 이름은 1자 이상 50자 이하여야 합니다.");
        }
        this.userId = userId;
        this.name = normalizedName;
        this.isDefault = isDefault;
    }

    public Long id() {
        return id;
    }

    public Long userId() {
        return userId;
    }

    public String name() {
        return name;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void updateName(String name) {
        String normalizedName = name == null ? null : name.strip();
        if (normalizedName == null || normalizedName.isEmpty() || normalizedName.length() > 50) {
            throw new IllegalArgumentException("관심 그룹 이름은 1자 이상 50자 이하여야 합니다.");
        }
        this.name = normalizedName;
    }
}
