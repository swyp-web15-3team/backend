package com.team3.exchange;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "exchange_rate_snapshots")
public class ExchangeRateSnapshot {
    @Id
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column(name = "rate_date", nullable = false)
    private LocalDate date;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode rates;

    protected ExchangeRateSnapshot() {
    }

    public ExchangeRateSnapshot(LocalDate date, JsonNode rates) {
        this.date = date;
        this.rates = rates;
    }

    public LocalDate getDate() {
        return date;
    }

    public JsonNode getRates() {
        return rates;
    }
}
