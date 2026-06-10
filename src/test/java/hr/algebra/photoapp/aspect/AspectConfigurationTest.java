package hr.algebra.photoapp.aspect;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class AspectConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void serviceLoggingAspectIsRegistered() {
        assertNotNull(context.getBean(ServiceLoggingAspect.class));
    }

    @Test
    void performanceMonitoringAspectIsRegistered() {
        assertNotNull(context.getBean(PerformanceMonitoringAspect.class));
    }
}
