package com.team3.whisky;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "whiskies")
public class Whisky {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private WhiskyCategory category;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "volume_ml", nullable = false)
    private Integer volumeMl;

    @Column(precision = 5, scale = 2)
    private BigDecimal abv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_id")
    private WhiskyOrigin origin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private WhiskyRegion region;

    protected Whisky() {
    }

    public Whisky(
        WhiskyCategory category,
        String name,
        Integer volumeMl,
        BigDecimal abv,
        WhiskyOrigin origin,
        WhiskyRegion region) {
        if (category == null) {
            throw new IllegalArgumentException("A category is required.");
        }
        if (name == null || name.isBlank() || name.length() > 255) {
            throw new IllegalArgumentException("A whisky name of 1 to 255 characters is required.");
        }
        if (volumeMl == null || volumeMl <= 0) {
            throw new IllegalArgumentException("A positive volume in millilitres is required.");
        }
        if (abv != null && (abv.compareTo(BigDecimal.ZERO) <= 0 || abv.compareTo(new BigDecimal("100")) > 0)) {
            throw new IllegalArgumentException("An ABV must be greater than 0 and at most 100.");
        }
        if (region != null && (origin == null || region.origin() != origin)) {
            throw new IllegalArgumentException("A region requires a matching origin.");
        }
        this.category = category;
        this.name = name;
        this.volumeMl = volumeMl;
        this.abv = abv;
        this.origin = origin;
        this.region = region;
    }

    public Long id() {
        return id;
    }

    public WhiskyCategory category() {
        return category;
    }

    public String name() {
        return name;
    }

    public Integer volumeMl() {
        return volumeMl;
    }

    public BigDecimal abv() {
        return abv;
    }

    public WhiskyOrigin origin() {
        return origin;
    }

    public WhiskyRegion region() {
        return region;
    }
}
