package hr.algebra.photoapp.metrics;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MetricsIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PhotoAppMetrics photoAppMetrics;

    @Autowired
    private PrometheusMeterRegistry prometheusRegistry;

    @Test
    void healthEndpointIsPublic() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("http://localhost:" + port + "/actuator/health", String.class);

        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

    @Test
    void prometheusEndpointExposesCustomMetrics() {
        photoAppMetrics.recordUpload();

        String scrape = prometheusRegistry.scrape();
        assertTrue(scrape.contains("photoapp_uploads_total"),
                "Custom metrics should be registered in Prometheus registry");

        ResponseEntity<String> response =
                restTemplate.getForEntity("http://localhost:" + port + "/actuator/prometheus", String.class);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("photoapp_uploads_total"));
        assertTrue(response.getBody().contains("photoapp_photos_stored"));
    }
}
