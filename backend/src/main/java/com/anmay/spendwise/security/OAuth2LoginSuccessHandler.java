package com.anmay.spendwise.security;

import com.anmay.spendwise.entity.AppUser;
import com.anmay.spendwise.service.OAuthAccountService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
  @org.springframework.beans.factory.annotation.Autowired private SessionService sessions;
  private final OAuthAccountService accountService;
  private final JwtService jwtService;
  private final AuthCookieService cookieService;
  private final String frontendUrl;

  public OAuth2LoginSuccessHandler(
      OAuthAccountService accountService,
      JwtService jwtService,
      AuthCookieService cookieService,
      @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
    this.accountService = accountService;
    this.jwtService = jwtService;
    this.cookieService = cookieService;
    this.frontendUrl = frontendUrl;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {
    OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
    AppUser user = accountService.upsertGoogleUser(oauth2User);
    var tokens = sessions.create(user);
    cookieService.writeToken(response, tokens.access());
    cookieService.writeRefresh(response, tokens.refresh());
    clearAuthenticationAttributes(request);
    if (request.getSession(false) != null) request.getSession(false).invalidate();
    String target =
        "same-origin".equalsIgnoreCase(frontendUrl)
            ? "/?oauth=success"
            : frontendUrl.replaceAll("/$", "") + "/?oauth=success";
    getRedirectStrategy().sendRedirect(request, response, target);
  }
}
