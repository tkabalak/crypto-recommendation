package com.example.crypto.domain.exception;

/**
 * Thrown when CSV import fails.
 */
public class DataImportException extends RuntimeException {
    public DataImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
