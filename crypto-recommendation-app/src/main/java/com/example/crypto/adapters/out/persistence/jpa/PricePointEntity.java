package com.example.crypto.adapters.out.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity representing a single crypto price point.
 *
 * <p>Uniqueness is enforced by (symbol, timestamp).
 */
@Entity
@Table(
        name = "price_points",
        indexes = {
                @Index(name = "idx_symbol_ts", columnList = "symbol,timestamp"),
                @Index(name = "idx_ts", columnList = "timestamp")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_symbol_ts", columnNames = {"symbol", "timestamp"})
        }
)
public class PricePointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, precision = 24, scale = 10)
    private BigDecimal priceUsd;

    protected PricePointEntity() {
    }

    /**
     * Creates a new price point.
     *
     * @param symbol    crypto symbol (e.g. BTC)
     * @param timestamp timestamp of the price point (UTC)
     * @param priceUsd  price in USD
     */
    public PricePointEntity(String symbol, Instant timestamp, BigDecimal priceUsd) {
        this.symbol = symbol;
        this.timestamp = timestamp;
        this.priceUsd = priceUsd;
    }

    /**
     * @return database identifier
     */
    public Long getId() {
        return id;
    }

    /**
     * @return crypto symbol
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * @return timestamp (UTC)
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * @return price in USD
     */
    public BigDecimal getPriceUsd() {
        return priceUsd;
    }
}
