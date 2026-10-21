package com.cab.config;

import com.stripe.exception.StripeException;
import org.springframework.http.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<Map<String, String>> err(HttpStatus s, String m) { return ResponseEntity.status(s).body(Map.of("error", m == null ? s.getReasonPhrase() : m)); }

    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> valid(MethodArgumentNotValidException e) {
        return err(HttpStatus.BAD_REQUEST, e.getBindingResult().getFieldErrors().stream().map(f -> f.getField() + ": " + f.getDefaultMessage()).reduce((a, b) -> a + "; " + b).orElse("Invalid input"));
    }
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<?> bad(IllegalArgumentException e) { return err(HttpStatus.BAD_REQUEST, e.getMessage()); }
    @ExceptionHandler(IllegalStateException.class) ResponseEntity<?> conflict(IllegalStateException e) { return err(HttpStatus.CONFLICT, e.getMessage()); }
    @ExceptionHandler(NoSuchElementException.class) ResponseEntity<?> nf(NoSuchElementException e) { return err(HttpStatus.NOT_FOUND, "Not found"); }
    @ExceptionHandler(AccessDeniedException.class) ResponseEntity<?> denied(AccessDeniedException e) { return err(HttpStatus.FORBIDDEN, e.getMessage()); }
    @ExceptionHandler(BadCredentialsException.class) ResponseEntity<?> cred(BadCredentialsException e) { return err(HttpStatus.UNAUTHORIZED, e.getMessage()); }
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class) ResponseEntity<?> lock(Exception e) { return err(HttpStatus.CONFLICT, "Ride was just taken by another driver"); }
    @ExceptionHandler(StripeException.class) ResponseEntity<?> stripe(StripeException e) { return err(HttpStatus.BAD_GATEWAY, "Payment provider error: " + e.getMessage()); }
}
