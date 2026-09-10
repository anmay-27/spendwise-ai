package com.anmay.spendwise.finance;

import com.anmay.spendwise.security.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/goals")
public class GoalController {
  public record GoalRequest(
      @NotBlank @Size(max = 100) String name,
      @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal targetAmount,
      @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal currentAmount,
      @NotNull @FutureOrPresent LocalDate targetDate,
      long version) {}

  private final GoalService service;
  private final CurrentUserService current;

  public GoalController(GoalService service, CurrentUserService current) {
    this.service = service;
    this.current = current;
  }

  @GetMapping
  public Object list() {
    return service.list(current.currentUserId());
  }

  @PostMapping
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public void create(@Valid @RequestBody GoalRequest r) {
    service.save(current.currentUserId(), null, r);
  }

  @PutMapping("/{id}")
  public void update(@PathVariable Long id, @Valid @RequestBody GoalRequest r) {
    service.save(current.currentUserId(), id, r);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(current.currentUserId(), id);
  }
}
