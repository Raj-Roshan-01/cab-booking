package com.cab.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "rides", indexes = { @Index(columnList = "status"), @Index(columnList = "rider_id"), @Index(columnList = "driver_id") })
@Getter @Setter @NoArgsConstructor
public class Ride {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private Long version;                       // optimistic lock: only one driver can win accept
    @ManyToOne(optional = false) @JoinColumn(name = "rider_id") private User rider;
    @ManyToOne @JoinColumn(name = "driver_id") private Driver driver;
    private double pickupLat, pickupLng, dropLat, dropLng;
    private String pickupAddress, dropAddress, promoCode;
    private double distanceKm, fare, discount;
    private int etaMinutes;
    @Enumerated(EnumType.STRING) private RideStatus status = RideStatus.REQUESTED;
    private Instant createdAt = Instant.now();
    @ElementCollection(fetch = FetchType.EAGER) private Set<Long> rejectedDrivers = new HashSet<>();
}
