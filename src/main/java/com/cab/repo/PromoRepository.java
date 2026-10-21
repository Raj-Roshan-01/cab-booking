package com.cab.repo;
import com.cab.model.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface PromoRepository extends JpaRepository<PromoCode, Long> {
    Optional<PromoCode> findByCodeIgnoreCaseAndActiveTrue(String code);
    boolean existsByCodeIgnoreCase(String code);
}
