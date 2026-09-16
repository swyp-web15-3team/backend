package com.team3.whisky;

import java.math.BigDecimal;
import java.time.Instant;

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
@Table(name = "price_histories")
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_product_id", nullable = false)
    private SaleProduct saleProduct;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    protected PriceHistory() {
    }

    public PriceHistory(SaleProduct saleProduct, BigDecimal price, String currencyCode, Instant collectedAt) {
        if (saleProduct == null) {
            throw new IllegalArgumentException("A sale product is required.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("A positive price is required.");
        }
        if (!"KRW".equals(currencyCode) && !"JPY".equals(currencyCode) && !"USD".equals(currencyCode)) {
            throw new IllegalArgumentException("A currency must be KRW, JPY, or USD.");
        }
        if (collectedAt == null) {
            throw new IllegalArgumentException("A collected-at time is required.");
        }
        this.saleProduct = saleProduct;
        this.price = price;
        this.currencyCode = currencyCode;
        this.collectedAt = collectedAt;
    }

    public Long id() {
        return id;
    }

    public SaleProduct saleProduct() {
        return saleProduct;
    }

    public BigDecimal price() {
        return price;
    }

    public String currencyCode() {
        return currencyCode;
    }

    public Instant collectedAt() {
        return collectedAt;
    }
}
