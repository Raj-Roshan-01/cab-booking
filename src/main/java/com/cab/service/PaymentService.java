package com.cab.service;

import com.cab.model.*;
import com.cab.repo.*;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository payments; private final RideRepository rides; private final ReceiptService receipts;
    @Value("${stripe.secret-key:}") String secret;
    @Value("${stripe.webhook-secret:}") String webhookSecret;
    @Value("${stripe.publishable-key:}") String publishable;

    public boolean demo() { return secret.isBlank(); }
    public Map<String, Object> config() { return Map.of("publishableKey", publishable, "demo", demo()); }

    private Ride owned(String email, Long id) {
        Ride r = rides.findById(id).orElseThrow();
        if (!r.getRider().getEmail().equals(email)) throw new AccessDeniedException("Not your ride");
        return r;
    }

    /** Ride completed -> backend computes final fare -> Stripe PaymentIntent. Without a Stripe key a demo intent is created. */
    @Transactional
    public Map<String, Object> createIntent(String email, Long rideId) throws StripeException {
        Ride r = owned(email, rideId);
        if (r.getStatus() != RideStatus.COMPLETED) throw new IllegalStateException("Ride is not completed yet");
        Payment p = payments.findByRideId(rideId).orElseGet(Payment::new);
        if (p.getStatus() == PaymentStatus.SUCCEEDED) throw new IllegalStateException("Already paid");
        String clientSecret;
        if (demo()) { p.setStripeIntentId("demo_" + UUID.randomUUID()); clientSecret = "demo"; }
        else {
            Stripe.apiKey = secret;
            PaymentIntent pi = PaymentIntent.create(PaymentIntentCreateParams.builder()
                .setAmount(Math.round(r.getFare() * 100)).setCurrency("inr").addPaymentMethodType("card")
                .putMetadata("rideId", rideId.toString()).build());
            p.setStripeIntentId(pi.getId()); clientSecret = pi.getClientSecret();
        }
        p.setRide(r); p.setAmount(r.getFare()); p.setStatus(PaymentStatus.PENDING);
        payments.save(p);
        return Map.of("clientSecret", clientSecret, "amount", r.getFare());
    }

    /** Demo mode only: simulates the Stripe webhook confirming the payment. */
    @Transactional
    public Map<String, Object> demoConfirm(String email, Long rideId) {
        if (!demo()) throw new IllegalStateException("Demo confirmation is disabled when Stripe is configured");
        owned(email, rideId);
        Payment p = payments.findByRideId(rideId).orElseThrow(() -> new IllegalStateException("Create a payment intent first"));
        markPaid(p);
        return Map.of("status", "SUCCEEDED", "receiptNo", p.getReceiptNo());
    }

    /** Stripe webhook: verifies the signature, then marks the payment paid/failed and sends the receipt. */
    @Transactional
    public void webhook(String payload, String signature) throws Exception {
        if (webhookSecret.isBlank()) throw new IllegalStateException("Webhook secret not configured");
        Event e = Webhook.constructEvent(payload, signature, webhookSecret);     // throws SignatureVerificationException
        if (!e.getType().startsWith("payment_intent.")) return;
        PaymentIntent pi = (PaymentIntent) e.getDataObjectDeserializer().getObject().orElseThrow();
        payments.findByStripeIntentId(pi.getId()).ifPresent(p -> {
            if ("payment_intent.succeeded".equals(e.getType())) markPaid(p);
            else if ("payment_intent.payment_failed".equals(e.getType())) { p.setStatus(PaymentStatus.FAILED); payments.save(p); }
        });
    }

    private void markPaid(Payment p) {
        if (p.getStatus() == PaymentStatus.SUCCEEDED) return;          // idempotent (Stripe retries webhooks)
        p.setStatus(PaymentStatus.SUCCEEDED);
        p.setReceiptNo("RCPT-" + p.getRide().getId() + "-" + System.currentTimeMillis() / 1000);
        payments.save(p);
        receipts.email(p.getRide().getId());
    }
}
