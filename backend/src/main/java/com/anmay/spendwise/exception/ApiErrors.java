package com.anmay.spendwise.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;

public final class ApiErrors {
  private static final ObjectMapper JSON = new ObjectMapper();

  private ApiErrors() {}

  public static void write(
      HttpServletRequest request, HttpServletResponse response, int status, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    JSON.writeValue(
        response.getWriter(),
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
