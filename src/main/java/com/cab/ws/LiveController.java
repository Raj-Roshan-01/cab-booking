package com.cab.ws;

import com.cab.dto.Dtos.LocationReq;
import com.cab.service.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.time.Instant;
import java.util.Map;

@Controller @RequiredArgsConstructor
public class LiveController {
    private final RideService rides;

    /** Client sends to /app/ride/{id}/sos ; admins subscribed to /topic/sos receive it. */
    @MessageMapping("/ride/{id}/sos") @SendTo("/topic/sos")
    public Map<String, Object> sos(@DestinationVariable Long id, Principal p) {
        return Map.of("rideId", id, "by", p.getName(), "time", Instant.now().toString());
    }

    /** Driver app streams GPS to /app/driver/location -> rider receives it on /topic/ride/{id}/location. */
    @MessageMapping("/driver/location")
    public void location(LocationReq r, Principal p) { rides.driverLocation(p.getName(), r.lat(), r.lng()); }
}
