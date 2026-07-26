package com.anmay.spendwise.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private final String frontendUrl;

    public OAuth2LoginFailureHandler(@Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String message = URLEncoder.encode("Google sign-in failed", StandardCharsets.UTF_8);
        String target = "same-origin".equalsIgnoreCase(frontendUrl)
                ? "/?authError=" + message
                : frontendUrl.replaceAll("/$", "") + "/?authError=" + message;
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
