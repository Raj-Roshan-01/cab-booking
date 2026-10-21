package com.cab.model;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "promo_codes") @Getter @Setter @NoArgsConstructor
public class PromoCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true, nullable = false) private String code;
    private int percentOff;
    private boolean active = true;
}
