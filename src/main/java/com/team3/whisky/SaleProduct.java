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

@Entity
@Table(name = "sale_products")
public class SaleProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "whisky_id")
    private Whisky whisky;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retailer_id", nullable = false)
    private Retailer retailer;

    @Column(name = "external_product_id", nullable = false, length = 255)
    private String externalProductId;

    @Column(name = "raw_name", nullable = false, length = 500)
    private String rawName;

    @Column(name = "raw_category", length = 100)
    private String rawCategory;

    @Column(name = "raw_spec", length = 255)
    private String rawSpec;

    @Column(name = "product_url", nullable = false)
    private String productUrl;

    @Column(name = "is_sold_out")
    private Boolean soldOut;

    protected SaleProduct() {
    }

    public SaleProduct(
        Whisky whisky,
        Retailer retailer,
        String externalProductId,
        String rawName,
        String rawCategory,
        String rawSpec,
        String productUrl,
        Boolean soldOut) {
        if (retailer == null) {
            throw new IllegalArgumentException("A retailer is required.");
        }
        if (productUrl == null || productUrl.isBlank()) {
            throw new IllegalArgumentException("A product URL is required.");
        }
        if (externalProductId == null || externalProductId.isBlank() || externalProductId.length() > 255) {
            throw new IllegalArgumentException("An external product ID of 1 to 255 characters is required.");
        }
        if (rawName == null || rawName.isBlank() || rawName.length() > 500) {
            throw new IllegalArgumentException("A raw product name of 1 to 500 characters is required.");
        }
        if (rawCategory != null && rawCategory.length() > 100) {
            throw new IllegalArgumentException("A raw category must be at most 100 characters.");
        }
        if (rawSpec != null && rawSpec.length() > 255) {
            throw new IllegalArgumentException("A raw specification must be at most 255 characters.");
        }
        this.externalProductId = externalProductId;
        this.rawName = rawName;
        this.rawCategory = rawCategory;
        this.rawSpec = rawSpec;
        this.whisky = whisky;
        this.retailer = retailer;
        this.productUrl = productUrl;
        this.soldOut = soldOut;
    }

    public Long id() {
        return id;
    }

    public Whisky whisky() {
        return whisky;
    }

    public Retailer retailer() {
        return retailer;
    }

    public String externalProductId() {
        return externalProductId;
    }

    public String rawName() {
        return rawName;
    }

    public String rawCategory() {
        return rawCategory;
    }

    public String rawSpec() {
        return rawSpec;
    }

    public String productUrl() {
        return productUrl;
    }

    public Boolean isSoldOut() {
        return soldOut;
    }
}
