package com.cab.controller;

import com.cab.dto.Dtos.*;
import com.cab.model.*;
import com.cab.repo.UserRepository;
import com.cab.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class AuthController {
    private final AuthService auth; private final UserRepository users;

    @PostMapping("/auth/register/rider") AuthRes rider(@Valid @RequestBody RegisterReq r) { return auth.register(r, Role.RIDER); }
    @PostMapping("/auth/register/driver") AuthRes driver(@Valid @RequestBody RegisterReq r) { return auth.register(r, Role.DRIVER); }
    @PostMapping("/auth/login") AuthRes login(@Valid @RequestBody LoginReq r) { return auth.login(r); }
    @GetMapping("/me") Map<String, String> me(Authentication a) {
        User u = users.findByEmail(a.getName()).orElseThrow();
        return Map.of("name", u.getName(), "email", u.getEmail(), "role", u.getRole().name());
    }
}
