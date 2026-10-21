package com.cab.service;

import com.cab.dto.Dtos.*;
import com.cab.model.*;
import com.cab.repo.*;
import com.cab.ws.Notifier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

import static com.cab.model.RideStatus.*;

@Service @RequiredArgsConstructor
public class RideService {
    private final RideRepository rides; private final UserRepository users; private final DriverRepository drivers;
    private final PromoRepository promos; private final PaymentRepository payments; private final RatingRepository ratings;
    private final FareService fares; private final Notifier notifier;

    static final double RADIUS_KM = 10;
    static final List<RideStatus> RIDER_ACTIVE = List.of(REQUESTED, ACCEPTED, ARRIVED, IN_PROGRESS);
    static final List<RideStatus> DRIVER_ACTIVE = List.of(ACCEPTED, ARRIVED, IN_PROGRESS);

    // ---------- helpers ----------
    public RideView view(Ride r) {
        boolean paid = payments.findByRideId(r.getId()).map(p -> p.getStatus() == PaymentStatus.SUCCEEDED).orElse(false);
        return RideView.of(r, paid, ratings.findByRideId(r.getId()).isPresent());
    }
    private Ride ride(Long id) { return rides.findById(id).orElseThrow(); }
    private Driver driver(String email) { return drivers.findByUserEmail(email).orElseThrow(); }
    private void push(Ride r) { notifier.send("/topic/ride/" + r.getId(), view(r)); }

    public Ride accessible(String email, Long id) {
        Ride r = ride(id);
        boolean ok = r.getRider().getEmail().equals(email) || (r.getDriver() != null && r.getDriver().getUser().getEmail().equals(email));
        if (!ok) throw new AccessDeniedException("Not your ride");
        return r;
    }

    // ---------- rider ----------
    public Map<String, Object> estimate(FareReq q) {
        double km = fares.distanceKm(q.pickupLat(), q.pickupLng(), q.dropLat(), q.dropLng());
        return Map.of("distanceKm", km, "etaMinutes", fares.eta(km), "fare", fares.fare(km));
    }

    @Transactional
    public RideView book(String email, BookReq q) {
        User u = users.findByEmail(email).orElseThrow();
        if (rides.findFirstByRiderEmailAndStatusIn(email, RIDER_ACTIVE).isPresent()) throw new IllegalStateException("You already have an active ride");
        double km = fares.distanceKm(q.pickupLat(), q.pickupLng(), q.dropLat(), q.dropLng());
        if (km < 0.1) throw new IllegalArgumentException("Pickup and drop are too close");
        double fare = fares.fare(km), discount = 0;
        if (q.promoCode() != null && !q.promoCode().isBlank()) {
            PromoCode p = promos.findByCodeIgnoreCaseAndActiveTrue(q.promoCode().trim()).orElseThrow(() -> new IllegalArgumentException("Invalid promo code"));
            discount = FareService.round2(fare * p.getPercentOff() / 100.0);
        }
        Ride r = new Ride();
        r.setRider(u); r.setPickupLat(q.pickupLat()); r.setPickupLng(q.pickupLng()); r.setDropLat(q.dropLat()); r.setDropLng(q.dropLng());
        r.setPickupAddress(q.pickupAddress()); r.setDropAddress(q.dropAddress()); r.setPromoCode(q.promoCode());
        r.setDistanceKm(km); r.setEtaMinutes(fares.eta(km)); r.setDiscount(discount); r.setFare(FareService.round2(fare - discount));
        rides.save(r);
        matchDrivers(r);
        return view(r);
    }

    /** Day 5 – matching: notify the 5 nearest available drivers within 10 km that have not rejected this ride. */
    void matchDrivers(Ride r) {
        drivers.findByAvailableTrue().stream()
            .filter(d -> !r.getRejectedDrivers().contains(d.getId()))
            .map(d -> Map.entry(d, FareService.haversine(d.getLat(), d.getLng(), r.getPickupLat(), r.getPickupLng())))
            .filter(e -> e.getValue() <= RADIUS_KM)
            .sorted(Map.Entry.comparingByValue()).limit(5)
            .forEach(e -> notifier.send("/topic/driver/" + e.getKey().getId(), view(r)));
    }

    public List<RideView> riderHistory(String email, int page, int size) {
        return rides.findByRiderEmailOrderByCreatedAtDesc(email, PageRequest.of(page, Math.min(size, 100))).stream().map(this::view).toList();
    }

    @Transactional
    public RideView cancel(String email, Long id) {
        Ride r = ride(id);
        if (!r.getRider().getEmail().equals(email)) throw new AccessDeniedException("Not your ride");
        if (!List.of(REQUESTED, ACCEPTED, ARRIVED).contains(r.getStatus())) throw new IllegalStateException("Ride can no longer be cancelled");
        r.setStatus(CANCELLED);
        if (r.getDriver() != null) { Driver d = r.getDriver(); d.setAvailable(true); drivers.save(d); }
        rides.save(r); push(r);
        return view(r);
    }

    public Map<String, Object> sos(String email, Long id) {
        Ride r = accessible(email, id);
        Map<String, Object> alert = Map.of("rideId", r.getId(), "by", email, "time", Instant.now().toString());
        notifier.send("/topic/sos", alert);
        notifier.send("/topic/ride/" + id + "/sos", alert);
        return alert;
    }

    // ---------- driver ----------
    public List<RideView> pendingFor(String email) {
        Driver d = driver(email);
        if (!d.isAvailable()) return List.of();
        return rides.findByStatus(REQUESTED).stream()
            .filter(r -> !r.getRejectedDrivers().contains(d.getId()))
            .filter(r -> FareService.haversine(d.getLat(), d.getLng(), r.getPickupLat(), r.getPickupLng()) <= RADIUS_KM)
            .map(this::view).toList();
    }

    @Transactional
    public RideView accept(String email, Long id) {
        Driver d = driver(email);
        if (!d.isAvailable()) throw new IllegalStateException("Go online before accepting rides");
        Ride r = ride(id);
        if (r.getStatus() != REQUESTED) throw new IllegalStateException("Ride is no longer available");
        r.setDriver(d); r.setStatus(ACCEPTED); d.setAvailable(false);
        drivers.save(d); rides.saveAndFlush(r);           // flush -> @Version check -> losing driver gets 409
        push(r);
        return view(r);
    }

    @Transactional
    public RideView reject(String email, Long id) {
        Driver d = driver(email); Ride r = ride(id);
        if (r.getStatus() != REQUESTED) throw new IllegalStateException("Ride is no longer available");
        r.getRejectedDrivers().add(d.getId()); rides.save(r);
        matchDrivers(r);                                  // offer to the next nearest drivers
        return view(r);
    }

    public RideView arrived(String e, Long id) { return step(e, id, ACCEPTED, ARRIVED); }
    public RideView start(String e, Long id) { return step(e, id, ARRIVED, IN_PROGRESS); }
    public RideView complete(String e, Long id) { return step(e, id, IN_PROGRESS, COMPLETED); }

    @Transactional
    RideView step(String email, Long id, RideStatus from, RideStatus to) {
        Ride r = ride(id);
        if (r.getDriver() == null || !r.getDriver().getUser().getEmail().equals(email)) throw new AccessDeniedException("Not your ride");
        if (r.getStatus() != from) throw new IllegalStateException("Ride must be " + from + " first");
        r.setStatus(to);
        if (to == COMPLETED) { Driver d = r.getDriver(); d.setEarnings(FareService.round2(d.getEarnings() + r.getFare())); d.setAvailable(true); drivers.save(d); }
        rides.save(r); push(r);
        return view(r);
    }

    @Transactional
    public void setAvailable(String email, boolean on) {
        Driver d = driver(email);
        if (on && rides.findFirstByDriverUserEmailAndStatusIn(email, DRIVER_ACTIVE).isPresent()) throw new IllegalStateException("Finish your current ride first");
        d.setAvailable(on); drivers.save(d);
    }

    @Transactional
    public void driverLocation(String email, double lat, double lng) {
        Driver d = driver(email); d.setLat(lat); d.setLng(lng); drivers.save(d);
        rides.findFirstByDriverUserEmailAndStatusIn(email, DRIVER_ACTIVE)
             .ifPresent(r -> notifier.send("/topic/ride/" + r.getId() + "/location", Map.of("lat", lat, "lng", lng)));
    }

    public List<RideView> driverHistory(String email, int page, int size) {
        return rides.findByDriverUserEmailOrderByCreatedAtDesc(email, PageRequest.of(page, Math.min(size, 100))).stream().map(this::view).toList();
    }

    public Map<String, Object> dashboard(String email) {
        Driver d = driver(email);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("driverId", d.getId()); m.put("name", d.getUser().getName()); m.put("available", d.isAvailable());
        m.put("earnings", d.getEarnings()); m.put("avgRating", d.getAvgRating()); m.put("ratingCount", d.getRatingCount());
        m.put("completedRides", rides.countByDriverUserEmailAndStatus(email, COMPLETED));
        m.put("activeRide", rides.findFirstByDriverUserEmailAndStatusIn(email, DRIVER_ACTIVE).map(this::view).orElse(null));
        return m;
    }
}
