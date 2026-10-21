package com.cab.repo;
import com.cab.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByRideId(Long rideId);
    List<Rating> findByRideDriverUserEmailOrderByCreatedAtDesc(String email);
}
