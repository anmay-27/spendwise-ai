package com.anmay.spendwise.controller;

import com.anmay.spendwise.finance.OperationsService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final OperationsService operations;

  public AdminController(OperationsService operations) {
    this.operations = operations;
  }

  @GetMapping("/overview")
  public Map<String, Object> overview() {
    return operations.overview();
  }
}
