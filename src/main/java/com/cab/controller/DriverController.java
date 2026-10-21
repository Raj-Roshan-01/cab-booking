package com.cab.controller;

import com.cab.dto.Dtos.*;
import com.cab.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequiredArgsConstructor
public class DriverController {
    private final RideService rides; private final RatingService ratings;

    @GetMapping("/api/driver/dashboard") Map<String, Object> dash(Authentication a) { return rides.dashboard(a.getName()); }
    @PostMapping("/api/driver/online") Map<String, Object> online(Authentication a, @RequestParam boolean available) { rides.setAvailable(a.getName(), available); return Map.of("available", available); }
    @PutMapping("/api/driver/location") Map<String, Object> loc(Authentication a, @RequestBody LocationReq r) { rides.driverLocation(a.getName(), r.lat(), r.lng()); return Map.of("ok", true); }
    @GetMapping("/api/driver/requests") List<RideView> requests(Authentication a) { return rides.pendingFor(a.getName()); }
    @PostMapping("/api/driver/rides/{id}/accept") RideView accept(Authentication a, @PathVariable Long id) { return rides.accept(a.getName(), id); }
    @PostMapping("/api/driver/rides/{id}/reject") RideView reject(Authentication a, @PathVariable Long id) { return rides.reject(a.getName(), id); }
    @PostMapping("/api/driver/rides/{id}/arrived") RideView arrived(Authentication a, @PathVariable Long id) { return rides.arrived(a.getName(), id); }
    @PostMapping("/api/driver/rides/{id}/start") RideView start(Authentication a, @PathVariable Long id) { return rides.start(a.getName(), id); }
    @PostMapping("/api/driver/rides/{id}/complete") RideView complete(Authentication a, @PathVariable Long id) { return rides.complete(a.getName(), id); }
    @GetMapping("/api/driver/history") List<RideView> history(Authentication a, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return rides.driverHistory(a.getName(), page, size); }
    @GetMapping("/api/driver/ratings") List<Map<String, Object>> myRatings(Authentication a) { return ratings.forDriver(a.getName()); }
    @GetMapping("/api/drivers/ranking") List<Map<String, Object>> ranking() { return ratings.ranking(); }
}
