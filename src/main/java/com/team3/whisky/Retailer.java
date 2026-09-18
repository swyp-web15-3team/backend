package com.team3.whisky;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "retailers")
public class Retailer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "is_duty_free", nullable = false)
    private boolean dutyFree;

    @Column(length = 255)
    private String address;

    protected Retailer() {
    }

    public Retailer(String name, String countryCode, boolean dutyFree, String address) {
        if (name == null || name.isBlank() || name.length() > 150) {
            throw new IllegalArgumentException("A retailer name of 1 to 150 characters is required.");
        }
        if (!"KR".equals(countryCode) && !"JP".equals(countryCode)) {
            throw new IllegalArgumentException("A retailer country must be KR or JP.");
        }
        if (address != null && address.length() > 255) {
            throw new IllegalArgumentException("An address must be at most 255 characters.");
        }
        this.name = name;
        this.countryCode = countryCode;
        this.dutyFree = dutyFree;
        this.address = address;
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String countryCode() {
        return countryCode;
    }

    public boolean isDutyFree() {
        return dutyFree;
    }

    public String address() {
        return address;
    }
}
