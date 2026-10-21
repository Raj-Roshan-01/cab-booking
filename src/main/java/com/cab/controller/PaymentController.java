package com.cab.controller;

import com.cab.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/payments") @RequiredArgsConstructor
public class PaymentController {
    private final PaymentService payments;

    @GetMapping("/config") Map<String, Object> config() { return payments.config(); }
    @PostMapping("/ride/{id}/intent") Map<String, Object> intent(Authentication a, @PathVariable Long id) throws Exception { return payments.createIntent(a.getName(), id); }
    @PostMapping("/ride/{id}/demo-confirm") Map<String, Object> demo(Authentication a, @PathVariable Long id) { return payments.demoConfirm(a.getName(), id); }

    @PostMapping("/webhook")
    ResponseEntity<String> webhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sig) {
        try { payments.webhook(payload, sig); return ResponseEntity.ok("ok"); }
        catch (Exception e) { return ResponseEntity.badRequest().body("invalid"); }
    }
}
