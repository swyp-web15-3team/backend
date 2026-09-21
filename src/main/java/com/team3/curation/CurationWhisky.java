package com.team3.curation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "curation_whiskies", uniqueConstraints = @UniqueConstraint(name = "uk_curation_whiskies_curation_whisky", columnNames = {
        "curation_id", "whisky_id"}))
public class CurationWhisky {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "curation_id", nullable = false)
    private Long curationId;

    @Column(name = "whisky_id", nullable = false)
    private Long whiskyId;

    protected CurationWhisky() {
    }

    public CurationWhisky(Long curationId, Long whiskyId) {
        this.curationId = curationId;
        this.whiskyId = whiskyId;
    }

    public Long id() {
        return id;
    }

    public Long curationId() {
        return curationId;
    }

    public Long whiskyId() {
        return whiskyId;
    }
}
