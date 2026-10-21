package com.cab.model;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "drivers", indexes = @Index(columnList = "available")) @Getter @Setter @NoArgsConstructor
public class Driver {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(optional = false) @JoinColumn(name = "user_id", unique = true) private User user;
    private String vehicleModel;
    private String plate;
    private double lat, lng;
    private boolean available;
    private double avgRating;
    private int ratingCount;
    private double earnings;
}
