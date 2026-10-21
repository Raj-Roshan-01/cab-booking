package com.cab.service;

import com.cab.dto.Dtos.*;
import com.cab.model.*;
import com.cab.repo.*;
import com.cab.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class AuthService {
    private final UserRepository users; private final DriverRepository drivers; private final PasswordEncoder enc; private final JwtService jwt;

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    @Transactional
    public AuthRes register(RegisterReq r, Role role) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw new IllegalStateException("Email already registered");
        if (role == Role.DRIVER && (blank(r.vehicleModel()) || blank(r.plate())))
            throw new IllegalArgumentException("Vehicle model and plate are required for drivers");
        User u = new User();
        u.setName(r.name()); u.setEmail(email); u.setPhone(r.phone()); u.setRole(role);
        u.setPassword(enc.encode(r.password()));            // BCrypt
        users.save(u);
        if (role == Role.DRIVER) {
            Driver d = new Driver(); d.setUser(u); d.setVehicleModel(r.vehicleModel()); d.setPlate(r.plate()); drivers.save(d);
        }
        return new AuthRes(jwt.create(email, role.name()), role.name(), u.getName());
    }

    public AuthRes login(LoginReq r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .filter(x -> x.getPassword() != null && enc.matches(r.password(), x.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return new AuthRes(jwt.create(u.getEmail(), u.getRole().name()), u.getRole().name(), u.getName());
    }

    /** Google OAuth2: create a rider account on first login, then issue our own JWT. */
    @Transactional
    public String oauthLogin(String email, String name) {
        User u = users.findByEmail(email.toLowerCase()).orElseGet(() -> {
            User n = new User(); n.setEmail(email.toLowerCase()); n.setName(name); n.setRole(Role.RIDER); return users.save(n);
        });
        return jwt.create(u.getEmail(), u.getRole().name());
    }
}
