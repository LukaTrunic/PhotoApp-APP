package hr.algebra.photoapp.metrics;

import hr.algebra.photoapp.repository.PhotoRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PhotoAppMetricsTest {

    private SimpleMeterRegistry registry;
    private PhotoRepository photoRepository;
    private ObjectProvider<PrometheusMeterRegistry> prometheusRegistry;
    private PhotoAppMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        photoRepository = mock(PhotoRepository.class);
        prometheusRegistry = mock(ObjectProvider.class);
        when(photoRepository.count()).thenReturn(3L);
        when(prometheusRegistry.getIfAvailable()).thenReturn(null);
        metrics = new PhotoAppMetrics(registry, prometheusRegistry, photoRepository);
        metrics.initializeGauge();
    }

    @Test
    void recordUpload_incrementsCounterAndRefreshesGauge() {
        when(photoRepository.count()).thenReturn(4L);

        metrics.recordUpload();

        assertEquals(1.0, registry.get("photoapp.uploads.total").counter().count());
        assertEquals(4.0, registry.get("photoapp.photos.stored").gauge().value());
    }

    @Test
    void recordDownload_incrementsDownloadCounter() {
        metrics.recordDownload();
        metrics.recordDownload();

        assertEquals(2.0, registry.get("photoapp.downloads.total").counter().count());
    }
}
