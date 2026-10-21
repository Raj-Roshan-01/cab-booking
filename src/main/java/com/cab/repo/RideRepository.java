package com.cab.repo;
import com.cab.model.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByRiderEmailOrderByCreatedAtDesc(String email, Pageable p);
    List<Ride> findByDriverUserEmailOrderByCreatedAtDesc(String email, Pageable p);
    List<Ride> findByStatus(RideStatus s);
    long countByStatusIn(Collection<RideStatus> s);
    Optional<Ride> findFirstByRiderEmailAndStatusIn(String email, Collection<RideStatus> s);
    Optional<Ride> findFirstByDriverUserEmailAndStatusIn(String email, Collection<RideStatus> s);
    long countByDriverUserEmailAndStatus(String email, RideStatus s);
}
