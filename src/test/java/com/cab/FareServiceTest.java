package com.cab;

import com.cab.service.FareService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FareServiceTest {
    final FareService f = new FareService();

    @Test void haversineDelhiToMumbaiIsAbout1150Km() { assertEquals(1150, FareService.haversine(28.61, 77.21, 19.07, 72.88), 40); }
    @Test void minimumFareApplies() { assertEquals(80.0, f.fare(0.2)); }
    @Test void fareGrowsWithDistance() { assertTrue(f.fare(20) > f.fare(5)); }
    @Test void etaUsesThirtyKmh() { assertEquals(60, f.eta(30)); }
}
