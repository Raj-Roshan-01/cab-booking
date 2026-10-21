package com.cab.repo;
import com.cab.model.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByUserEmailOrderByCreatedAtDesc(String email);
    List<SupportTicket> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
}
