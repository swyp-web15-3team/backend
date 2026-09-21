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

    @Column(name = "product_url", nullable = false)
    private String productUrl;

    @Column(name = "is_sold_out")
    private Boolean soldOut;

    protected SaleProduct() {
    }

    public SaleProduct(Whisky whisky, Retailer retailer, String productUrl, Boolean soldOut) {
        if (retailer == null) {
            throw new IllegalArgumentException("A retailer is required.");
        }
        if (productUrl == null || productUrl.isBlank()) {
            throw new IllegalArgumentException("A product URL is required.");
        }
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

    public String productUrl() {
        return productUrl;
    }

    public Boolean isSoldOut() {
        return soldOut;
    }
}
