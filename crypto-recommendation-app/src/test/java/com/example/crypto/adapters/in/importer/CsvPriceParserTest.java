package com.example.crypto.adapters.in.importer;

import com.example.crypto.domain.exception.DataImportException;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for {@link CsvPriceParser}.
 */
class CsvPriceParserTest {

    private final CsvPriceParser parser = new CsvPriceParser();

    @Test
    void shouldParseTaskFormat_header_timestampSymbolPrice_epochMillis() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("csv/BTC_values.csv")) {
            assertNotNull(in, "Test CSV resource not found");

            List<CsvPriceParser.ParsedRow> rows = parser.parse(in, "BTC");
            assertEquals(9, rows.size());

            var first = rows.get(0);
            assertEquals("BTC", first.symbol());
            assertEquals(Instant.ofEpochMilli(1641009600000L), first.timestamp());
            assertEquals(0, first.priceUsd().compareTo(new BigDecimal("46813.21")));

            var last = rows.get(rows.size() - 1);
            assertEquals("BTC", last.symbol());
            assertEquals(Instant.ofEpochMilli(1641243600000L), last.timestamp());
            assertEquals(0, last.priceUsd().compareTo(new BigDecimal("45922.01")));
        }
    }

    @Test
    void shouldFallbackToFilenameSymbolWhenSymbolBlank() throws Exception {
        String csv = """
                timestamp,symbol,price
                1641009600000,,46813.21
                """;

        try (InputStream in = new java.io.ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))) {
            List<CsvPriceParser.ParsedRow> rows = parser.parse(in, "BTC");
            assertEquals(1, rows.size());
            assertEquals("BTC", rows.get(0).symbol());
        }
    }

    @Test
    void shouldParseHeaderCaseInsensitive_epochSeconds() throws Exception {
        String csv = """
                TIMESTAMP,SYMBOL,PRICE
                1641009600,eth,3000.00
                """;

        try (InputStream in = new java.io.ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))) {
            List<CsvPriceParser.ParsedRow> rows = parser.parse(in, "ETH");
            assertEquals(1, rows.size());
            assertEquals("ETH", rows.get(0).symbol());
            assertEquals(Instant.ofEpochSecond(1641009600L), rows.get(0).timestamp());
            assertEquals(0, rows.get(0).priceUsd().compareTo(new BigDecimal("3000.00")));
        }
    }

    @Test
    void shouldParseIsoInstantTimestamp() throws Exception {
        String csv = """
                timestamp,symbol,price
                2026-01-01T00:00:00Z,BTC,50000.12
                """;

        try (InputStream in = new java.io.ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))) {
            List<CsvPriceParser.ParsedRow> rows = parser.parse(in, "BTC");
            assertEquals(1, rows.size());
            assertEquals(Instant.parse("2026-01-01T00:00:00Z"), rows.get(0).timestamp());
        }
    }

    @Test
    void shouldParseHeaderlessFormat_timestampPrice() throws Exception {
        String csv = """
                1641009600000,46813.21
                1641020400000,46979.61
                """;

        try (InputStream in = new java.io.ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))) {
            List<CsvPriceParser.ParsedRow> rows = parser.parse(in, "BTC");
            assertEquals(2, rows.size());
            assertEquals("BTC", rows.get(0).symbol());
            assertEquals(Instant.ofEpochMilli(1641009600000L), rows.get(0).timestamp());
            assertEquals(0, rows.get(0).priceUsd().compareTo(new BigDecimal("46813.21")));
        }
    }

    @Test
    void shouldParseHeaderlessFormat_timestampSymbolPrice() throws Exception {
        String csv = """
                1641009600000,BTC,46813.21
                1641020400000,BTC,46979.61
                """;

        try (InputStream in = new java.io.ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))) {
            List<CsvPriceParser.ParsedRow> rows = parser.parse(in, "IGNORED");
            assertEquals(2, rows.size());
            assertEquals("BTC", rows.get(0).symbol());
        }
    }

    @Test
    void shouldThrowDataImportExceptionForUnsupportedFormat() {
        String csv = """
                foo,bar
                1,2
                """;
        try (InputStream in = new java.io.ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))) {
            assertThrows(DataImportException.class, () -> parser.parse(in, "BTC"));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void shouldThrowDataImportExceptionForInvalidPrice() {
        String csv = """
                timestamp,symbol,price
                1641009600000,BTC,NOT_A_NUMBER
                """;

        try (InputStream in = new java.io.ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))) {
            assertThrows(DataImportException.class, () -> parser.parse(in, "BTC"));
        } catch (Exception e) {
            fail(e);
        }
    }
}
