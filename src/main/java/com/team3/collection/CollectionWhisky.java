package com.team3.collection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "collection_whiskies")
public class CollectionWhisky {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "collection_id", nullable = false)
    private Long collectionId;

    @Column(name = "whisky_id", nullable = false)
    private Long whiskyId;

    protected CollectionWhisky() {
    }

    public CollectionWhisky(Long collectionId, Long whiskyId) {
        this.collectionId = collectionId;
        this.whiskyId = whiskyId;
    }
}
