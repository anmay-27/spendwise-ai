package com.anmay.spendwise.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
  private final String frontendUrl;

  public HomeController(@Value("${app.frontend-url}") String frontendUrl) {
    this.frontendUrl = frontendUrl;
  }

  @GetMapping("/")
  public String frontend() {
    return "same-origin".equals(frontendUrl) ? "forward:/index.html" : "redirect:" + frontendUrl;
  }
}
