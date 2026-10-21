package com.cab.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "payments") @Getter @Setter @NoArgsConstructor
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(optional = false) @JoinColumn(name = "ride_id", unique = true) private Ride ride;
    private double amount;
    @Column(unique = true) private String stripeIntentId;
    @Enumerated(EnumType.STRING) private PaymentStatus status = PaymentStatus.PENDING;
    private String receiptNo;
    private Instant createdAt = Instant.now();
}
