package hr.algebra.photoapp.repository;

import hr.algebra.photoapp.model.PackageChange;
import hr.algebra.photoapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Repository Pattern
// Abstracts data access for PackageChange entities
@Repository
public interface PackageChangeRepository extends JpaRepository<PackageChange, Long> {

    
    List<PackageChange> findByEffectiveDateAndAppliedFalse(LocalDate effectiveDate);


    void deleteByUser(User user);



}
