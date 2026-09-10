package com.anmay.notifications;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class NotificationErrors {
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> error(Exception error, HttpServletRequest request) {
    int status = error instanceof ResponseStatusException ex ? ex.getStatusCode().value() : 500;
    String message =
        error instanceof ResponseStatusException ex && ex.getReason() != null
            ? ex.getReason()
            : "Unexpected server error";
    return ResponseEntity.status(status)
        .body(
            Map.of(
                "timestamp",
                Instant.now().toString(),
                "status",
                status,
                "error",
                HttpStatus.valueOf(status).name(),
                "message",
                message,
                "path",
                request.getRequestURI()));
  }
}
