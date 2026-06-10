package hr.algebra.photoapp.scheduler;

import hr.algebra.photoapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UploadResetScheduler {

    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyUploads() {

        userRepository.findAll().forEach(user -> {
            user.setUploadsToday(0);
        });

        userRepository.flush();

        System.out.println("Daily upload counters reset");
    }
}

