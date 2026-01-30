package com.example.crypto.domain.exception;

/**
 * Thrown when a client requests a crypto symbol that is not supported by the system.
 */
public class UnsupportedCryptoException extends RuntimeException {
    private final String symbol;

    public UnsupportedCryptoException(String symbol) {
        super("Crypto symbol '%s' is not supported".formatted(symbol));
        this.symbol = symbol;
    }

    /**
     * @return unsupported symbol provided by the client
     */
    public String getSymbol() {
        return symbol;
    }
}
