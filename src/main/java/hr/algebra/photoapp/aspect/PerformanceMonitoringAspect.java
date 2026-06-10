package hr.algebra.photoapp.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Measures execution time of expensive operations (image I/O and processing).
 */
@Aspect
@Component
public class PerformanceMonitoringAspect {

    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);
    private static final long SLOW_OPERATION_MS = 500;

    @Pointcut("execution(* hr.algebra.photoapp.service.impl.PhotoServiceImpl.upload(..))")
    public void photoUpload() {
    }

    @Pointcut("execution(* hr.algebra.photoapp.service.impl.PhotoServiceImpl.getPhotoBytes(..))")
    public void photoDownload() {
    }

    @Pointcut("execution(* hr.algebra.photoapp.service.impl.PhotoServiceImpl.getPhotoWithFilters(..))")
    public void filteredDownload() {
    }

    @Around("photoUpload() || photoDownload() || filteredDownload()")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startMs = System.currentTimeMillis();

        try {
            return joinPoint.proceed();
        } finally {
            long durationMs = System.currentTimeMillis() - startMs;
            String message = String.format("[AOP-PERF] %s.%s took %d ms",
                    joinPoint.getSignature().getDeclaringType().getSimpleName(),
                    joinPoint.getSignature().getName(),
                    durationMs);

            if (durationMs >= SLOW_OPERATION_MS) {
                log.warn("{} (slow operation)", message);
            } else {
                log.info(message);
            }
        }
    }
}
