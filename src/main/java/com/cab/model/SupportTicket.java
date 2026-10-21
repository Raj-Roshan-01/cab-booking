package com.cab.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "support_tickets") @Getter @Setter @NoArgsConstructor
public class SupportTicket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id") private User user;
    private String subject;
    @Column(length = 2000) private String message;
    private String status = "OPEN";
    private Instant createdAt = Instant.now();
}
