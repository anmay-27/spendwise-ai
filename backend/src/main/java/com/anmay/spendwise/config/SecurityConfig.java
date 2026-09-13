package com.anmay.spendwise.config;

import com.anmay.spendwise.security.CookieBearerTokenResolver;
import com.anmay.spendwise.security.OAuth2LoginFailureHandler;
import com.anmay.spendwise.security.OAuth2LoginSuccessHandler;
import com.anmay.spendwise.security.SpaCsrfTokenRequestHandler;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      ObjectProvider<ClientRegistrationRepository> registrations,
      OAuth2LoginSuccessHandler successHandler,
      OAuth2LoginFailureHandler failureHandler,
      JwtAuthenticationConverter jwtAuthenticationConverter,
      com.anmay.spendwise.security.SessionService sessions,
      org.springframework.data.redis.core.StringRedisTemplate redis,
      @Value("${app.rate-limit.enabled:true}") boolean rateEnabled)
      throws Exception {

    CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    csrfRepository.setCookiePath("/");

    http.cors(Customizer.withDefaults())
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(csrfRepository)
                    .ignoringRequestMatchers("/api/checkout/webhook")
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
        .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(
                        "/api/health",
                        "/api/checkout/webhook",
                        "/api/auth/csrf",
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/logout",
                        "/actuator/health/**",
                        "/actuator/prometheus",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers("/oauth2/**", "/login/**", "/error")
                    .permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .oauth2ResourceServer(
            resourceServer ->
                resourceServer
                    .bearerTokenResolver(new CookieBearerTokenResolver())
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                    .authenticationEntryPoint(
                        (request, response, exception) -> {
                          com.anmay.spendwise.exception.ApiErrors.write(
                              request, response, 401, "Please log in to continue");
                        }))
        .exceptionHandling(
            exceptions ->
                exceptions.accessDeniedHandler(
                    (request, response, exception) -> {
                      com.anmay.spendwise.exception.ApiErrors.write(
                          request, response, 403, "You do not have permission for this action");
                    }));

    if (registrations.getIfAvailable() != null) {
      http.oauth2Login(
          oauth2 -> oauth2.successHandler(successHandler).failureHandler(failureHandler));
    }

    http.addFilterAfter(
        new com.anmay.spendwise.security.SessionValidationFilter(sessions),
        org.springframework.security.oauth2.server.resource.web.authentication
            .BearerTokenAuthenticationFilter.class);
    if (rateEnabled)
      http.addFilterAfter(
          new com.anmay.spendwise.security.RateLimitFilter(redis),
          org.springframework.security.oauth2.server.resource.web.authentication
              .BearerTokenAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }

  @Bean
  SecretKey jwtSecretKey(@Value("${app.security.jwt-secret}") String secret) {
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < 32) {
      throw new IllegalStateException("APP_JWT_SECRET must contain at least 32 bytes");
    }
    return new SecretKeySpec(bytes, "HmacSHA256");
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKey secretKey) {
    return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey));
  }

  @Bean
  JwtDecoder jwtDecoder(SecretKey secretKey) {
    var decoder =
        NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(
        org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer("spendwise"));
    return decoder;
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
    authorities.setAuthoritiesClaimName("roles");
    authorities.setAuthorityPrefix("ROLE_");

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authorities);
    converter.setPrincipalClaimName("sub");
    return converter;
  }
}
