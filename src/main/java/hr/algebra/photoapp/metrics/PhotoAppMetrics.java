package hr.algebra.photoapp.metrics;

import hr.algebra.photoapp.repository.PhotoRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * LO9: application-specific metrics exposed via /actuator/prometheus.
 */
@Component
public class PhotoAppMetrics {

    private final PhotoRepository photoRepository;
    private final Counter uploadsTotal;
    private final Counter downloadsTotal;
    private final Counter deletionsTotal;
    private final Counter registrationsTotal;
    private final AtomicLong storedPhotos = new AtomicLong();

    public PhotoAppMetrics(MeterRegistry registry,
                           ObjectProvider<PrometheusMeterRegistry> prometheusRegistry,
                           PhotoRepository photoRepository) {
        this.photoRepository = photoRepository;
        PrometheusMeterRegistry prometheus = prometheusRegistry.getIfAvailable();
        MeterRegistry targetRegistry = prometheus != null ? prometheus : registry;

        this.uploadsTotal = Counter.builder("photoapp.uploads.total")
                .description("Successful photo uploads")
                .register(targetRegistry);

        this.downloadsTotal = Counter.builder("photoapp.downloads.total")
                .description("Photo download requests")
                .register(targetRegistry);

        this.deletionsTotal = Counter.builder("photoapp.deletions.total")
                .description("Photos deleted")
                .register(targetRegistry);

        this.registrationsTotal = Counter.builder("photoapp.registrations.total")
                .description("New user registrations")
                .register(targetRegistry);

        Gauge.builder("photoapp.photos.stored", storedPhotos, AtomicLong::get)
                .description("Current number of photos in the database")
                .register(targetRegistry);
    }

    @PostConstruct
    void initializeGauge() {
        refreshStoredPhotoCount();
    }

    public void recordUpload() {
        uploadsTotal.increment();
        refreshStoredPhotoCount();
    }

    public void recordDownload() {
        downloadsTotal.increment();
    }

    public void recordDeletion() {
        deletionsTotal.increment();
        refreshStoredPhotoCount();
    }

    public void recordRegistration() {
        registrationsTotal.increment();
    }

    public void refreshStoredPhotoCount() {
        storedPhotos.set(photoRepository.count());
    }
}
