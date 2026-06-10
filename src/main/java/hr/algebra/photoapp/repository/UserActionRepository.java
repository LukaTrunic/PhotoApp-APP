package hr.algebra.photoapp.repository;

import hr.algebra.photoapp.model.User;
import hr.algebra.photoapp.model.UserAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// Repository Pattern
// Abstracts data access for UserAction entities
@Repository
public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    
    List<UserAction> findByUsernameOrderByTimestampDesc(String username);
    
    Page<UserAction> findAllByOrderByTimestampDesc(Pageable pageable);
    
    long countByUsername(String username);
    
    List<UserAction> findTop10ByUsernameOrderByTimestampDesc(String username);

    void deleteByUser(User user);

}
