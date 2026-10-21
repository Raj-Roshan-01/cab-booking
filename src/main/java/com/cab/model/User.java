package com.cab.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "users") @Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String name;
    @Column(unique = true, nullable = false) private String email;
    private String password;            // null for Google-only accounts
    private String phone;
    @Enumerated(EnumType.STRING) private Role role;
    private Instant createdAt = Instant.now();
}
