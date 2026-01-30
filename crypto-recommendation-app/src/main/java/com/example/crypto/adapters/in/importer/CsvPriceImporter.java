package com.example.crypto.adapters.in.importer;

import com.example.crypto.adapters.out.persistence.jpa.PricePointEntity;
import com.example.crypto.adapters.out.persistence.repository.PricePointRepository;
import com.example.crypto.config.properties.AppImportProperties;
import com.example.crypto.domain.exception.DataImportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Imports crypto price points from CSV files located under classpath.
 *
 * <p>Import runs on application startup and persists data into the database.
 * This prevents reading CSV files on every request.
 */
@Component
@EnableConfigurationProperties(AppImportProperties.class)
public class CsvPriceImporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CsvPriceImporter.class);

    private final AppImportProperties props;
    private final ResourcePatternResolver resolver;
    private final PricePointRepository repository;

    public CsvPriceImporter(AppImportProperties props,
                            ResourcePatternResolver resolver,
                            PricePointRepository repository) {
        this.props = props;
        this.resolver = resolver;
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.enabled()) {
            log.info("CSV import disabled (app.import.enabled=false).");

            return;
        }

        if (props.cleanBeforeImport()) {
            log.warn("Cleaning DB before import (app.import.clean-before-import=true)...");
            repository.deleteAllInBatch();
        } else if (repository.count() > 0) {
            log.info("DB already contains data (count={}). Skipping import.", repository.count());
            return;
        }

        try {
            Resource[] resources = resolver.getResources(props.resourcePattern());
            if (resources.length == 0) {
                log.warn("No CSV resources found for pattern: {}", props.resourcePattern());
                return;
            }

            CsvPriceParser parser = new CsvPriceParser();

            long startNanos = System.nanoTime();
            int batchSize = Math.max(1, props.batchSize());

            Map<String, Integer> perSymbol = new HashMap<>();
            long totalImported = 0;

            List<PricePointEntity> batch = new ArrayList<>(batchSize);

            for (Resource resource : resources) {
                String filename = Optional.ofNullable(resource.getFilename()).orElse("UNKNOWN");
                String fallbackSymbol = symbolFromFilename(filename);

                log.info("Importing CSV: {} (fallbackSymbol={})", filename, fallbackSymbol);

                try (InputStream in = resource.getInputStream()) {
                    var rows = parser.parse(in, fallbackSymbol);

                    for (var r : rows) {
                        batch.add(new PricePointEntity(r.symbol(), r.timestamp(), r.priceUsd()));
                        perSymbol.merge(r.symbol(), 1, Integer::sum);
                        totalImported++;

                        if (batch.size() >= batchSize) {
                            repository.saveAll(batch);

                            repository.flush();
                            batch.clear();
                        }
                    }
                }
            }

            if (!batch.isEmpty()) {
                repository.saveAll(batch);

                repository.flush();
                batch.clear();
            }

            long tookMs = (System.nanoTime() - startNanos) / 1_000_000;

            log.info("Imported {} price points from {} CSV file(s) in {} ms.", totalImported, resources.length, tookMs);
            log.info("Import summary per symbol: {}", perSymbol);

        } catch (Exception e) {
            throw new DataImportException("CSV import failed", e);
        }
    }

    /**
     * Derives a symbol from a CSV filename.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code BTC_values.csv -> BTC}</li>
     *   <li>{@code eth_values.csv -> ETH}</li>
     *   <li>{@code ADA.csv -> ADA}</li>
     * </ul>
     */
    private String symbolFromFilename(String filename) {
        var upper = filename.toUpperCase(Locale.ROOT);

        int idx = upper.indexOf("_VALUES.CSV");
        if (idx > 0) {
            return upper.substring(0, idx);
        }
        int dot = upper.lastIndexOf('.');

        return dot > 0 ? upper.substring(0, dot) : upper;
    }
}
