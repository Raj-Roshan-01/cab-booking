package com.cab.controller;

import com.cab.dto.Dtos.*;
import com.cab.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/rides") @RequiredArgsConstructor
public class RideController {
    private final RideService rides; private final ReceiptService receipts; private final RatingService ratings;

    @PostMapping("/estimate") Map<String, Object> estimate(@RequestBody FareReq r) { return rides.estimate(r); }
    @PostMapping RideView book(Authentication a, @Valid @RequestBody BookReq r) { return rides.book(a.getName(), r); }
    @GetMapping List<RideView> history(Authentication a, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return rides.riderHistory(a.getName(), page, size); }
    @GetMapping("/{id}") RideView get(Authentication a, @PathVariable Long id) { return rides.view(rides.accessible(a.getName(), id)); }
    @PostMapping("/{id}/cancel") RideView cancel(Authentication a, @PathVariable Long id) { return rides.cancel(a.getName(), id); }
    @PostMapping("/{id}/sos") Map<String, Object> sos(Authentication a, @PathVariable Long id) { return rides.sos(a.getName(), id); }
    @PostMapping("/{id}/rating") Map<String, Object> rate(Authentication a, @PathVariable Long id, @Valid @RequestBody RatingReq r) { return ratings.rate(a.getName(), id, r); }

    @GetMapping("/{id}/receipt") ResponseEntity<byte[]> receipt(Authentication a, @PathVariable Long id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt-" + id + ".pdf").body(receipts.pdf(a.getName(), id));
    }
    @PostMapping("/{id}/email-receipt") Map<String, String> email(Authentication a, @PathVariable Long id) {
        rides.accessible(a.getName(), id); receipts.email(id); return Map.of("status", "queued");
    }
}
