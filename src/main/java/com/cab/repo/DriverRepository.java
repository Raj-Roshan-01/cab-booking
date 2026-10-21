package com.cab.repo;
import com.cab.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByUserEmail(String email);
    List<Driver> findByAvailableTrue();
    List<Driver> findTop10ByRatingCountGreaterThanOrderByAvgRatingDescRatingCountDesc(int min);
}
