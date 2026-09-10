package com.anmay.gateway;

import java.util.UUID;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.*;

@SpringBootApplication
public class GatewayApplication {
  public static void main(String[] args) {
    SpringApplication.run(GatewayApplication.class, args);
  }

  @Bean
  GlobalFilter requestId() {
    return (exchange, chain) -> {
      String id = UUID.randomUUID().toString();
      exchange.getResponse().getHeaders().set("X-Request-Id", id);
      return chain.filter(
          exchange
              .mutate()
              .request(
                  exchange
                      .getRequest()
                      .mutate()
                      .headers(
                          h -> {
                            h.remove("X-User-Id");
                            h.remove("X-Forwarded-For");
                            h.remove("Forwarded");
                            h.set("X-Request-Id", id);
                          })
                      .build())
              .build());
    };
  }
}
