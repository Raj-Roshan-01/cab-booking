package com.cab.service;

import com.cab.dto.Dtos.RatingReq;
import com.cab.model.*;
import com.cab.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratings; private final RideRepository rides; private final DriverRepository drivers;

    @Transactional @CacheEvict(value = "ranking", allEntries = true)
    public Map<String, Object> rate(String email, Long rideId, RatingReq q) {
        Ride r = rides.findById(rideId).orElseThrow();
        if (!r.getRider().getEmail().equals(email)) throw new AccessDeniedException("Not your ride");
        if (r.getStatus() != RideStatus.COMPLETED) throw new IllegalStateException("You can rate only completed rides");
        if (ratings.findByRideId(rideId).isPresent()) throw new IllegalStateException("Ride already rated");
        Rating x = new Rating(); x.setRide(r); x.setStars(q.stars()); x.setComment(q.comment()); ratings.save(x);
        Driver d = r.getDriver();
        d.setAvgRating(FareService.round2((d.getAvgRating() * d.getRatingCount() + q.stars()) / (d.getRatingCount() + 1)));
        d.setRatingCount(d.getRatingCount() + 1);
        drivers.save(d);
        return Map.of("driverAvgRating", d.getAvgRating(), "ratingCount", d.getRatingCount());
    }

    /** Driver ranking (cached – Day 12 optimisation; evicted whenever a rating arrives). */
    @Cacheable("ranking")
    public List<Map<String, Object>> ranking() {
        List<Map<String, Object>> out = new ArrayList<>();
        int i = 1;
        for (Driver d : drivers.findTop10ByRatingCountGreaterThanOrderByAvgRatingDescRatingCountDesc(0))
            out.add(Map.of("rank", i++, "driver", d.getUser().getName(), "avgRating", d.getAvgRating(), "ratings", d.getRatingCount()));
        return out;
    }

    public List<Map<String, Object>> forDriver(String email) {
        return ratings.findByRideDriverUserEmailOrderByCreatedAtDesc(email).stream()
            .map(x -> Map.<String, Object>of("rideId", x.getRide().getId(), "stars", x.getStars(), "comment", x.getComment() == null ? "" : x.getComment(), "at", x.getCreatedAt().toString())).toList();
    }
}
