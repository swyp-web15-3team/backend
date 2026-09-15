package com.team3.user;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user_agreements", uniqueConstraints = @UniqueConstraint(name = "uk_user_agreements_user_type", columnNames = {
        "user_id", "agreement_type"}))
public class UserAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "agreement_type", nullable = false, length = 32)
    private AgreementType type;

    @Column(nullable = false)
    private boolean agreed;

    @CreatedDate
    @Column(name = "agreed_at", nullable = false, updatable = false)
    private LocalDateTime agreedAt;

    protected UserAgreement() {
    }

    public UserAgreement(Long userId, AgreementType type, boolean agreed) {
        if (userId == null || type == null) {
            throw new IllegalArgumentException("User ID and agreement type are required.");
        }
        this.userId = userId;
        this.type = type;
        this.agreed = agreed;
    }

    public Long userId() {
        return userId;
    }

    public AgreementType type() {
        return type;
    }

    public boolean agreed() {
        return agreed;
    }

    public LocalDateTime agreedAt() {
        return agreedAt;
    }
}
