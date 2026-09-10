package com.anmay.spendwise.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handle(Exception ex, HttpServletRequest request) {
    int status = 500;
    String message = "Unexpected server error";
    if (ex instanceof ResponseStatusException e) {
      status = e.getStatusCode().value();
      message = e.getReason() == null ? "Request failed" : e.getReason();
    } else if (ex instanceof MethodArgumentNotValidException e) {
      status = 400;
      message =
          e.getBindingResult().getFieldErrors().stream()
              .findFirst()
              .map(f -> f.getField() + ": " + f.getDefaultMessage())
              .orElse("Invalid request");
    } else if (ex instanceof IllegalArgumentException
        || ex instanceof java.time.DateTimeException
        || ex instanceof org.springframework.web.bind.ServletRequestBindingException
        || ex instanceof org.springframework.http.converter.HttpMessageNotReadableException
        || ex
            instanceof
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException) {
      status = 400;
      message = "Invalid request";
    } else if (ex instanceof org.springframework.security.core.AuthenticationException) {
      status = 401;
      message = "Invalid credentials";
    } else if (ex instanceof org.springframework.security.access.AccessDeniedException) {
      status = 403;
      message = "Access denied";
    } else if (ex instanceof org.springframework.dao.DataIntegrityViolationException
        || ex instanceof org.springframework.orm.ObjectOptimisticLockingFailureException) {
      status = 409;
      message = "Request conflicts with existing data";
    }
    if (status == 500)
      org.slf4j.LoggerFactory.getLogger(getClass())
          .error("Request failed path={}", request.getRequestURI(), ex);
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
