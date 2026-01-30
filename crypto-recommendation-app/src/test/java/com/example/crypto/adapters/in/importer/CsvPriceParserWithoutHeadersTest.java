package com.example.crypto.adapters.in.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.crypto.domain.exception.DataImportException;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Covers legacy "no headers" parsing path inside {@link CsvPriceParser} (tryParseWithoutHeaders).
 */
class CsvPriceParserWithoutHeadersTest {

  private final CsvPriceParser parser = new CsvPriceParser();

  @Test
  void shouldParseWithoutHeaders_timestampPrice_twoColumns() throws Exception {
    String csv = """
1641009600000,46813.21
1641020400000,46979.61
""";

    var rows = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "BTC");
    assertThat(rows).hasSize(2);
    assertThat(rows.get(0).symbol()).isEqualTo("BTC");
    assertThat(rows.get(0).timestamp()).isEqualTo(Instant.ofEpochMilli(1641009600000L));
    assertThat(rows.get(0).priceUsd()).isEqualByComparingTo(new BigDecimal("46813.21"));
  }

  @Test
  void shouldParseWithoutHeaders_timestampSymbolPrice_threeColumns() throws Exception {
    String csv = """
1641009600000,eth,3000.00
1641020400000,ETH,3010.00
""";

    var rows = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "IGNORED");
    assertThat(rows).hasSize(2);
    assertThat(rows.get(0).symbol()).isEqualTo("ETH");
    assertThat(rows.get(0).priceUsd()).isEqualByComparingTo(new BigDecimal("3000.00"));
  }

  @Test
  void invalidWithoutHeadersFormat_shouldThrowDataImportException() {
    // only 1 column -> tryParseWithoutHeaders returns null -> overall parse fails
    String csv = """
1641009600000
""";
    assertThatThrownBy(() ->
        parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "BTC"))
        .isInstanceOf(DataImportException.class);
  }
}
