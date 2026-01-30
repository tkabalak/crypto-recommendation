package com.example.crypto.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for CSV import.
 *
 * @param enabled           whether import runs on startup
 * @param cleanBeforeImport whether to clear DB before import
 * @param resourcePattern   classpath pattern for CSV resources
 * @param batchSize         database insert batch size
 */
@ConfigurationProperties(prefix = "app.import")
public record AppImportProperties(
        boolean enabled,
        boolean cleanBeforeImport,
        String resourcePattern,
        int batchSize
) {
}
