package com.team3.whisky;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "whisky_regions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_whisky_regions_origin_name", columnNames = {"origin_id", "name"}),
        @UniqueConstraint(name = "uk_whisky_regions_id_origin", columnNames = {"id", "origin_id"})
})
public class WhiskyRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_id", nullable = false)
    private WhiskyOrigin origin;

    @Column(nullable = false, length = 100)
    private String name;

    protected WhiskyRegion() {
    }

    public WhiskyRegion(WhiskyOrigin origin, String name) {
        if (origin == null) {
            throw new IllegalArgumentException("An origin is required.");
        }
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("A region name of 1 to 100 characters is required.");
        }
        this.origin = origin;
        this.name = name;
    }

    public Long id() {
        return id;
    }

    public WhiskyOrigin origin() {
        return origin;
    }

    public String name() {
        return name;
    }
}
