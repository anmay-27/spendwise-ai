package com.anmay.spendwise.finance;

import com.anmay.spendwise.dto.Responses.TransactionView;
import com.anmay.spendwise.entity.TransactionType;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionCrudController {
  private final TransactionCrudService service;

  public TransactionCrudController(TransactionCrudService service) {
    this.service = service;
  }

  @GetMapping("/search")
  public Page<TransactionView> search(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) TransactionType type,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "occurredAt") String sort,
      @RequestParam(defaultValue = "desc") String direction) {
    return service.search(query, type, categoryId, from, to, page, size, sort, direction);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionView create(@Valid @RequestBody TransactionRequest request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public TransactionView update(
      @PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
