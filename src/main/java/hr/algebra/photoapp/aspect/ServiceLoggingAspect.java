package hr.algebra.photoapp.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Logs entry to important business operations without mixing logging into service code.
 */
@Aspect
@Component
public class ServiceLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ServiceLoggingAspect.class);

    @Pointcut("execution(* hr.algebra.photoapp.service.impl.PhotoServiceImpl.upload(..))")
    public void photoUpload() {
    }

    @Pointcut("execution(* hr.algebra.photoapp.service.impl.PhotoServiceImpl.deletePhoto(..))")
    public void photoDelete() {
    }

    @Pointcut("execution(* hr.algebra.photoapp.service.impl.PhotoServiceImpl.updatePhoto(..))")
    public void photoUpdate() {
    }

    @Pointcut("execution(* hr.algebra.photoapp.service.impl.UserServiceImpl.registerUser(String, String, String, ..))")
    public void userRegistration() {
    }

    @Pointcut("photoUpload() || photoDelete() || photoUpdate() || userRegistration()")
    public void auditedServiceMethods() {
    }

    @Before("auditedServiceMethods()")
    public void logMethodEntry(JoinPoint joinPoint) {
        String user = currentUsername();
        log.info("[AOP-LOG] User '{}' -> {}.{}",
                user,
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName());
    }

    @AfterReturning(pointcut = "auditedServiceMethods()", returning = "result")
    public void logMethodSuccess(JoinPoint joinPoint, Object result) {
        log.info("[AOP-LOG] Completed {}.{} (result type: {})",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                result == null ? "void" : result.getClass().getSimpleName());
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        return authentication.getName();
    }
}
