package com.cab.dto;

import com.cab.model.*;
import jakarta.validation.constraints.*;
import java.time.Instant;

public final class Dtos {
    private Dtos() {}
    public record RegisterReq(@NotBlank String name, @Email @NotBlank String email,
            @Size(min = 8, message = "Password must be at least 8 characters") String password,
            String phone, String vehicleModel, String plate) {}
    public record LoginReq(@NotBlank String email, @NotBlank String password) {}
    public record AuthRes(String token, String role, String name) {}
    public record FareReq(double pickupLat, double pickupLng, double dropLat, double dropLng) {}
    public record BookReq(@NotNull Double pickupLat, @NotNull Double pickupLng, @NotNull Double dropLat, @NotNull Double dropLng,
            String pickupAddress, String dropAddress, String promoCode) {}
    public record LocationReq(double lat, double lng) {}
    public record RatingReq(@Min(1) @Max(5) int stars, String comment) {}
    public record TicketReq(@NotBlank String subject, @NotBlank String message) {}
    public record PromoReq(@NotBlank String code, @Min(1) @Max(100) int percentOff) {}

    public record RideView(Long id, String status, String rider, Long driverId, String driverName, String vehicle, String plate,
            double pickupLat, double pickupLng, double dropLat, double dropLng, String pickupAddress, String dropAddress,
            double distanceKm, int etaMinutes, double fare, double discount, Instant createdAt, boolean paid, boolean rated) {
        public static RideView of(Ride r, boolean paid, boolean rated) {
            Driver d = r.getDriver();
            return new RideView(r.getId(), r.getStatus().name(), r.getRider().getName(),
                    d == null ? null : d.getId(), d == null ? null : d.getUser().getName(),
                    d == null ? null : d.getVehicleModel(), d == null ? null : d.getPlate(),
                    r.getPickupLat(), r.getPickupLng(), r.getDropLat(), r.getDropLng(), r.getPickupAddress(), r.getDropAddress(),
                    r.getDistanceKm(), r.getEtaMinutes(), r.getFare(), r.getDiscount(), r.getCreatedAt(), paid, rated);
        }
    }
}
