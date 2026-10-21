package com.cab.repo;
import com.cab.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRideId(Long rideId);
    Optional<Payment> findByStripeIntentId(String id);
    @Query("select coalesce(sum(p.amount),0) from Payment p where p.status = com.cab.model.PaymentStatus.SUCCEEDED")
    double totalRevenue();
}
