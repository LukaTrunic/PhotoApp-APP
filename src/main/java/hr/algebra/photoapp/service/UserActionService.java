package hr.algebra.photoapp.service;

import hr.algebra.photoapp.model.User;
import hr.algebra.photoapp.model.UserAction;
import org.springframework.data.domain.Page;

import java.util.List;

// Service Layer Pattern
// Business logic for user action logging and auditing
public interface UserActionService {
    
    void logAction(User user, String action, String details, String ipAddress);
    
    void logAction(String username, String action, String details);
    
    List<UserAction> getUserActions(String username);
    
    Page<UserAction> getAllActions(int page, int size);
    
    long getActionCount(String username);
    
    List<UserAction> getRecentActions(String username, int limit);

    void deleteAllByUser(User user);

}
