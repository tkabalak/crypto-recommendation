package com.example.crypto.adapters.out.persistence.repository.projection;

import java.math.BigDecimal;

/**
 * Projection for min/max aggregation queries.
 */
public interface SymbolMinMaxProjection {

    /**
     * @return crypto symbol
     */
    String getSymbol();

    /**
     * @return aggregated minimum price
     */
    BigDecimal getMinPrice();

    /**
     * @return aggregated maximum price
     */
    BigDecimal getMaxPrice();
}
