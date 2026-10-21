package com.cab;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end: register -> book -> match -> accept -> ride -> pay (demo) -> rate -> ranking -> receipt -> admin. */
@SpringBootTest @AutoConfigureMockMvc
class CabFlowTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper om;

    String call(MockHttpServletRequestBuilder b, String token, Object body, int expect) throws Exception {
        if (token != null) b.header("Authorization", "Bearer " + token);
        if (body != null) b.contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body));
        return mvc.perform(b).andExpect(status().is(expect)).andReturn().getResponse().getContentAsString();
    }
    String token(String url, Map<String, ?> body) throws Exception { return om.readTree(call(post(url), null, body, 200)).get("token").asText(); }

    @Test void fullRideLifecycle() throws Exception {
        String rider = token("/api/auth/register/rider", Map.of("name", "Ria", "email", "ria@test.com", "password", "password1"));
        String drv = token("/api/auth/register/driver", Map.of("name", "Dev", "email", "dev@test.com", "password", "password1", "vehicleModel", "Swift", "plate", "DL1"));
        call(post("/api/auth/register/rider"), null, Map.of("name", "X", "email", "ria@test.com", "password", "password1"), 409);   // duplicate
        call(post("/api/auth/login"), null, Map.of("email", "ria@test.com", "password", "wrong"), 401);
        call(get("/api/rides"), null, null, 401);
        call(get("/api/admin/stats"), rider, null, 403);

        call(put("/api/driver/location"), drv, Map.of("lat", 28.61, "lng", 77.21), 200);
        call(post("/api/driver/online?available=true"), drv, null, 200);

        long id = om.readTree(call(post("/api/rides"), rider, Map.of("pickupLat", 28.61, "pickupLng", 77.21, "dropLat", 28.70, "dropLng", 77.10,
                "pickupAddress", "A", "dropAddress", "B", "promoCode", "WELCOME10"), 200)).get("id").asLong();
        assertEquals(1, om.readTree(call(get("/api/driver/requests"), drv, null, 200)).size());

        for (String step : new String[]{"accept", "arrived", "start", "complete"}) call(post("/api/driver/rides/" + id + "/" + step), drv, null, 200);

        call(post("/api/payments/ride/" + id + "/intent"), rider, null, 200);
        call(post("/api/payments/ride/" + id + "/demo-confirm"), rider, null, 200);
        call(post("/api/rides/" + id + "/rating"), rider, Map.of("stars", 5, "comment", "great"), 200);
        call(post("/api/rides/" + id + "/rating"), rider, Map.of("stars", 4), 409);       // one rating per ride

        JsonNode rank = om.readTree(call(get("/api/drivers/ranking"), rider, null, 200));
        assertEquals(5.0, rank.get(0).get("avgRating").asDouble());
        assertTrue(call(get("/api/rides/" + id + "/receipt"), rider, null, 200).startsWith("%PDF"));

        JsonNode dash = om.readTree(call(get("/api/driver/dashboard"), drv, null, 200));
        assertTrue(dash.get("earnings").asDouble() > 0);

        String admin = token("/api/auth/login", Map.of("email", "admin@cab.com", "password", "Admin@123"));
        assertTrue(om.readTree(call(get("/api/admin/stats"), admin, null, 200)).get("revenue").asDouble() > 0);
    }
}
