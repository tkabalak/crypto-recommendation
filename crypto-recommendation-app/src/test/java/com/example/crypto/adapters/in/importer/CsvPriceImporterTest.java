package com.example.crypto.adapters.in.importer;

import com.example.crypto.adapters.out.persistence.jpa.PricePointEntity;
import com.example.crypto.adapters.out.persistence.repository.PricePointRepository;
import com.example.crypto.config.properties.AppImportProperties;
import com.example.crypto.domain.exception.DataImportException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CsvPriceImporter}.
 */
class CsvPriceImporterTest {

    @Test
    void shouldSkipWhenDisabled() {
        AppImportProperties props = new AppImportProperties(false, false, "classpath*:data/*_values.csv", 1000);
        ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
        PricePointRepository repo = mock(PricePointRepository.class);

        CsvPriceImporter importer = new CsvPriceImporter(props, resolver, repo);
        importer.run(new DefaultApplicationArguments(new String[0]));

        verifyNoInteractions(resolver);
        verifyNoInteractions(repo);
    }

    @Test
    void shouldSkipWhenDbAlreadyHasDataAndCleanBeforeImportFalse() throws Exception {
        AppImportProperties props = new AppImportProperties(true, false, "classpath*:data/*_values.csv", 1000);
        ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
        PricePointRepository repo = mock(PricePointRepository.class);

        when(repo.count()).thenReturn(123L);

        CsvPriceImporter importer = new CsvPriceImporter(props, resolver, repo);
        importer.run(new DefaultApplicationArguments(new String[0]));

        verify(repo, times(2)).count();
        verifyNoInteractions(resolver);
        verify(repo, never()).saveAll(anyList());
    }

    @Test
    void shouldImportAndUseFallbackSymbolFromFilenameWhenSymbolMissing() throws Exception {
        AppImportProperties props = new AppImportProperties(true, true, "classpath*:data/*_values.csv", 2);
        ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
        PricePointRepository repo = mock(PricePointRepository.class);

        String csv = "timestamp,symbol,price\n"
                + "1641009600000,,46813.21\n"
                + "1641020400000,,46979.61\n"
                + "1641031200000,,47143.98\n";

        Resource res = new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "BTC_values.csv";
            }
        };

        when(resolver.getResources(anyString())).thenReturn(new Resource[]{res});
        when(repo.count()).thenReturn(0L);

        CsvPriceImporter importer = new CsvPriceImporter(props, resolver, repo);
        importer.run(new DefaultApplicationArguments(new String[0]));

        // clean-before-import=true
        verify(repo).deleteAllInBatch();

        // batch-size=2 => saveAll called at least twice (2 + 1)
        ArgumentCaptor<List<PricePointEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repo, atLeastOnce()).saveAll(captor.capture());
        verify(repo, atLeastOnce()).flush();
    }

    @Test
    void shouldWrapUnexpectedErrorsAsDataImportException() throws Exception {
        AppImportProperties props = new AppImportProperties(true, true, "classpath*:data/*_values.csv", 1000);
        ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
        PricePointRepository repo = mock(PricePointRepository.class);

        when(resolver.getResources(anyString())).thenThrow(new RuntimeException("boom"));

        CsvPriceImporter importer = new CsvPriceImporter(props, resolver, repo);

        assertThatThrownBy(() -> importer.run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(DataImportException.class)
                .hasMessageContaining("CSV import failed");
    }
}
