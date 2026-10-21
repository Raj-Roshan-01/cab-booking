package com.cab.config;

import com.cab.model.*;
import com.cab.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users; private final PromoRepository promos; private final PasswordEncoder enc;
    @Value("${app.admin.email}") String adminEmail;
    @Value("${app.admin.password}") String adminPassword;

    @Override public void run(String... args) {
        if (!users.existsByEmail(adminEmail)) {
            User a = new User(); a.setName("Admin"); a.setEmail(adminEmail); a.setPassword(enc.encode(adminPassword)); a.setRole(Role.ADMIN);
            users.save(a);
        }
        if (!promos.existsByCodeIgnoreCase("WELCOME10")) { PromoCode p = new PromoCode(); p.setCode("WELCOME10"); p.setPercentOff(10); promos.save(p); }
    }
}
