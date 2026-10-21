package com.cab.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/** Brute-force protection: max 30 auth calls per IP per minute. */
public class RateLimitFilter extends OncePerRequestFilter {
    private final ConcurrentHashMap<String, long[]> hits = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        if (req.getRequestURI().startsWith("/api/auth/")) {
            long now = System.currentTimeMillis();
            long[] w = hits.compute(req.getRemoteAddr(), (k, v) -> (v == null || now - v[0] > 60_000) ? new long[]{now, 1} : new long[]{v[0], v[1] + 1});
            if (w[1] > 30) {
                res.setStatus(429);
                res.setContentType("application/json");
                res.getWriter().write("{\"error\":\"Too many requests, try again in a minute\"}");
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
