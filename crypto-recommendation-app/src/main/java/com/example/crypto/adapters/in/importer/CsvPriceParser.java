package com.example.crypto.adapters.in.importer;

import com.example.crypto.domain.exception.DataImportException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * CSV parser for the test task format.
 * <p>
 * Expected header (case-insensitive):
 * - timestamp,symbol,price
 * <p>
 * Examples:
 * timestamp,symbol,price
 * 1641009600000,BTC,46813.21
 * <p>
 * Timestamp supports:
 * - epoch milliseconds (preferred, 13 digits)
 * - epoch seconds (10 digits)
 * - ISO-8601 instant (e.g. 2026-01-01T00:00:00Z)
 * <p>
 * Note: For backward compatibility, headerless CSV is also supported:
 * - timestamp,price
 * - timestamp,symbol,price
 */
class CsvPriceParser {

    record ParsedRow(String symbol, Instant timestamp, BigDecimal priceUsd) {
    }

    List<ParsedRow> parse(InputStream inputStream, String fallbackSymbol) {
        Objects.requireNonNull(inputStream, "inputStream");
        Objects.requireNonNull(fallbackSymbol, "fallbackSymbol");

        try {
            byte[] bytes = inputStream.readAllBytes();

            // 1) Preferred: parse with headers
            List<ParsedRow> withHeaders = tryParseWithHeaders(bytes, fallbackSymbol);
            if (withHeaders != null) return withHeaders;

            // 2) Fallback: parse without headers (legacy)
            List<ParsedRow> withoutHeaders = tryParseWithoutHeaders(bytes, fallbackSymbol);
            if (withoutHeaders != null) return withoutHeaders;

            throw new DataImportException("CSV parsing failed (unsupported format) for symbol " + fallbackSymbol, null);
        } catch (IOException e) {
            throw new DataImportException("Failed to read CSV bytes for symbol " + fallbackSymbol, e);
        }
    }

    private List<ParsedRow> tryParseWithHeaders(byte[] bytes, String fallbackSymbol) {
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setTrim(true)
                    .setIgnoreEmptyLines(true)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(reader);

            HeaderIndexes header = HeaderIndexes.from(parser.getHeaderMap());
            if (!header.isPresent()) {
                return null; // not a header CSV
            }

            List<ParsedRow> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                String tsRaw = record.get(header.timestampIdx());
                String symbolRaw = record.get(header.symbolIdx());
                String priceRaw = record.get(header.priceIdx());

                String symbol = (symbolRaw == null || symbolRaw.isBlank())
                        ? fallbackSymbol
                        : symbolRaw.trim().toUpperCase(Locale.ROOT);

                Instant ts = parseInstant(tsRaw);
                BigDecimal price = new BigDecimal(priceRaw.trim());

                rows.add(new ParsedRow(symbol, ts, price));
            }
            return rows;
        } catch (Exception e) {
            return null;
        }
    }

    private List<ParsedRow> tryParseWithoutHeaders(byte[] bytes, String fallbackSymbol) {
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setTrim(true)
                    .setIgnoreEmptyLines(true)
                    .build()
                    .parse(reader);

            List<ParsedRow> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                if (record.size() < 2) {
                    throw new IllegalArgumentException("Expected at least 2 columns (timestamp,price)");
                }
                String tsRaw = record.get(0);
                String symbolRaw = null;
                String priceRaw;

                if (record.size() >= 3) {
                    symbolRaw = record.get(1);
                    priceRaw = record.get(2);
                } else {
                    priceRaw = record.get(1);
                }

                String symbol = (symbolRaw == null || symbolRaw.isBlank())
                        ? fallbackSymbol
                        : symbolRaw.trim().toUpperCase(Locale.ROOT);

                Instant ts = parseInstant(tsRaw);
                BigDecimal price = new BigDecimal(priceRaw.trim());

                rows.add(new ParsedRow(symbol, ts, price));
            }
            return rows;
        } catch (Exception e) {
            return null;
        }
    }

    private Instant parseInstant(String raw) {
        String v = raw.trim();
        // digits -> epoch seconds/millis
        if (v.matches("^\\d{10,}$")) {
            long n = Long.parseLong(v);
            // heuristic: millis if >= 13 digits
            if (v.length() >= 13) {
                return Instant.ofEpochMilli(n);
            }
            return Instant.ofEpochSecond(n);
        }
        // ISO-8601
        return Instant.parse(v);
    }

    /**
     * Case-insensitive header matcher for required columns.
     */
    private record HeaderIndexes(Integer timestampIdx, Integer symbolIdx, Integer priceIdx) {

        static HeaderIndexes from(Map<String, Integer> headerMap) {
            if (headerMap == null || headerMap.isEmpty()) {
                return new HeaderIndexes(null, null, null);
            }
            Integer ts = null;
            Integer sym = null;
            Integer price = null;

            for (Map.Entry<String, Integer> e : headerMap.entrySet()) {
                String key = e.getKey();
                if (key == null) continue;
                if (key.equalsIgnoreCase("timestamp")) ts = e.getValue();
                if (key.equalsIgnoreCase("symbol")) sym = e.getValue();
                if (key.equalsIgnoreCase("price")) price = e.getValue();
            }
            return new HeaderIndexes(ts, sym, price);
        }

        boolean isPresent() {
            return timestampIdx != null && symbolIdx != null && priceIdx != null;
        }
    }
}
