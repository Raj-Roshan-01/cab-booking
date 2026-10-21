package com.cab.controller;

import com.cab.dto.Dtos.*;
import com.cab.model.*;
import com.cab.repo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequiredArgsConstructor
public class SupportAdminController {
    private final TicketRepository tickets; private final UserRepository users; private final PromoRepository promos;
    private final RideRepository rides; private final PaymentRepository payments; private final DriverRepository drivers;

    private Map<String, Object> t(SupportTicket x) {
        return Map.of("id", x.getId(), "user", x.getUser().getEmail(), "subject", x.getSubject(), "message", x.getMessage(), "status", x.getStatus(), "createdAt", x.getCreatedAt().toString());
    }

    @PostMapping("/api/support") Map<String, Object> open(Authentication a, @Valid @RequestBody TicketReq r) {
        SupportTicket x = new SupportTicket(); x.setUser(users.findByEmail(a.getName()).orElseThrow()); x.setSubject(r.subject()); x.setMessage(r.message());
        return t(tickets.save(x));
    }
    @GetMapping("/api/support") List<Map<String, Object>> mine(Authentication a) { return tickets.findByUserEmailOrderByCreatedAtDesc(a.getName()).stream().map(this::t).toList(); }

    @GetMapping("/api/admin/stats") Map<String, Object> stats() {
        return Map.of("users", users.count(), "drivers", drivers.count(), "rides", rides.count(),
            "activeRides", rides.countByStatusIn(List.of(RideStatus.REQUESTED, RideStatus.ACCEPTED, RideStatus.ARRIVED, RideStatus.IN_PROGRESS)),
            "revenue", payments.totalRevenue(), "openTickets", tickets.countByStatus("OPEN"));
    }
    @GetMapping("/api/admin/users") List<Map<String, Object>> users() {
        return users.findAll().stream().map(u -> Map.<String, Object>of("id", u.getId(), "name", u.getName(), "email", u.getEmail(), "role", u.getRole().name())).toList();
    }
    @GetMapping("/api/admin/tickets") List<Map<String, Object>> allTickets() { return tickets.findAllByOrderByCreatedAtDesc().stream().map(this::t).toList(); }
    @PostMapping("/api/admin/tickets/{id}/resolve") Map<String, Object> resolve(@PathVariable Long id) {
        SupportTicket x = tickets.findById(id).orElseThrow(); x.setStatus("RESOLVED"); return t(tickets.save(x));
    }
    @GetMapping("/api/admin/promos") List<PromoCode> listPromos() { return promos.findAll(); }
    @PostMapping("/api/admin/promos") PromoCode addPromo(@Valid @RequestBody PromoReq r) {
        if (promos.existsByCodeIgnoreCase(r.code())) throw new IllegalStateException("Promo code exists");
        PromoCode p = new PromoCode(); p.setCode(r.code().toUpperCase()); p.setPercentOff(r.percentOff()); return promos.save(p);
    }
}
