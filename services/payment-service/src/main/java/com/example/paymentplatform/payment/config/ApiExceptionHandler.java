package com.example.paymentplatform.payment.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Surfaces DB and other errors in JSON (enable only with profile {@code supabase} for debugging).
 */
@RestControllerAdvice
@Profile("supabase")
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccess(DataAccessException e) {
        log.error("Database error", e);
        return ResponseEntity.internalServerError().body(body(e.getClass().getSimpleName(), rootCauseMessage(e)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception e) {
        log.error("Request failed", e);
        return ResponseEntity.internalServerError().body(body(e.getClass().getSimpleName(), rootCauseMessage(e)));
    }

    private static Map<String, Object> body(String error, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", error);
        m.put("message", message);
        return m;
    }

    private static String rootCauseMessage(Throwable e) {
        Throwable c = e;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        String m = c.getMessage();
        return m != null ? m : "unknown";
    }
}
