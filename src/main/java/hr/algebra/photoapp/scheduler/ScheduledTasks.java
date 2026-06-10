package hr.algebra.photoapp.scheduler;

import hr.algebra.photoapp.model.User;
import hr.algebra.photoapp.repository.UserRepository;
import hr.algebra.photoapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

// Scheduled Task Pattern + Observer Pattern (triggered by time)
// Handles scheduled tasks like resetting daily upload counters and applying package changes
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);
    
    private final UserRepository userRepository;
    private final UserService userService;

    // Reset daily upload counters at midnight
    @Scheduled(cron = "0 0 0 * * *")
    public void resetDailyUploads() {
        log.info("Resetting daily upload counters...");
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        for (User user : userRepository.findAll()) {
            if (user.getLastUploadDate() != null && 
                user.getLastUploadDate().isBefore(LocalDate.now())) {
                user.setUploadsToday(0);
                userRepository.save(user);
            }
        }
        
        log.info("Daily upload counters reset complete");
    }

    // Apply pending package changes
    @Scheduled(cron = "0 5 0 * * *")
    public void applyPackageChanges() {
        log.info("Applying pending package changes...");
        
        try {
            userService.applyPendingPackageChanges();
            log.info("Package changes applied successfully");
        } catch (Exception e) {
            log.error("Error applying package changes: " + e.getMessage(), e);
        }
    }
}
