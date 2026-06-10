package hr.algebra.photoapp.service.impl;

import hr.algebra.photoapp.model.User;
import hr.algebra.photoapp.model.UserAction;
import hr.algebra.photoapp.repository.UserActionRepository;
import hr.algebra.photoapp.service.UserActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Service Layer Pattern + Singleton (Spring manages as singleton)
// Implements action logging business logic
@Service
@RequiredArgsConstructor
@Transactional
public class UserActionServiceImpl implements UserActionService {

    private final UserActionRepository userActionRepository;

    @Override
    public void logAction(User user, String action, String details, String ipAddress) {
        UserAction userAction = UserAction.builder()
                .user(user)
                .username(user != null ? user.getUsername() : "Anonymous")
                .action(action)
                .details(details)
                .timestamp(LocalDateTime.now())
                .ipAddress(ipAddress)
                .build();
        
        userActionRepository.save(userAction);
    }

    @Override
    public void logAction(String username, String action, String details) {
        UserAction userAction = UserAction.builder()
                .username(username)
                .action(action)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
        
        userActionRepository.save(userAction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAction> getUserActions(String username) {
        return userActionRepository.findByUsernameOrderByTimestampDesc(username);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserAction> getAllActions(int page, int size) {
        return userActionRepository.findAllByOrderByTimestampDesc(
                PageRequest.of(page, size)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long getActionCount(String username) {
        return userActionRepository.countByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAction> getRecentActions(String username, int limit) {
        return userActionRepository.findTop10ByUsernameOrderByTimestampDesc(username);
    }

    @Override
    @Transactional
    public void deleteAllByUser(User user) {
        userActionRepository.deleteByUser(user);
    }

}
