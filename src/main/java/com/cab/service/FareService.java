package com.cab.service;

import org.springframework.stereotype.Service;

@Service
public class FareService {
    static final double BASE = 50, PER_KM = 12, PER_MIN = 1.5, MIN_FARE = 80, AVG_SPEED_KMH = 30, ROAD_FACTOR = 1.3;

    public static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double h = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
        return 6371.0 * 2 * Math.asin(Math.sqrt(h));
    }
    public static double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    /** Approximate road distance (straight line x 1.3). Swap for Google Distance Matrix / OSRM if keys are available. */
    public double distanceKm(double a, double b, double c, double d) { return round2(haversine(a, b, c, d) * ROAD_FACTOR); }
    public int eta(double km) { return (int) Math.ceil(km / AVG_SPEED_KMH * 60); }
    public double fare(double km) { return round2(Math.max(MIN_FARE, BASE + PER_KM * km + PER_MIN * eta(km))); }
}
