package com.anmay.spendwise.finance;

import com.anmay.spendwise.dto.Responses.TransactionView;
import com.anmay.spendwise.entity.*;
import com.anmay.spendwise.events.EventOutbox;
import com.anmay.spendwise.repository.*;
import com.anmay.spendwise.security.CurrentUserService;
import com.anmay.spendwise.service.ViewMapper;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TransactionCrudService {
  private final ExpenseTransactionRepository txs;
  private final CategoryRepository categories;
  private final CurrentUserService current;
  private final EventOutbox events;

  public TransactionCrudService(
      ExpenseTransactionRepository txs,
      CategoryRepository categories,
      CurrentUserService current,
      EventOutbox events) {
    this.txs = txs;
    this.categories = categories;
    this.current = current;
    this.events = events;
  }

  @Transactional(readOnly = true)
  public Page<TransactionView> search(
      String query,
      TransactionType type,
      Long categoryId,
      LocalDate from,
      LocalDate to,
      int page,
      int size,
      String sort,
      String direction) {
    if (page < 0
        || size < 1
        || size > 100
        || !Set.of("occurredAt", "amount", "merchantName").contains(sort)
        || !Set.of("asc", "desc").contains(direction))
      throw new IllegalArgumentException("Invalid pagination");
    if (from != null && to != null && from.isAfter(to))
      throw new IllegalArgumentException("Invalid dates");
    Long uid = current.currentUserId();
    Specification<ExpenseTransaction> spec =
        (root, q, cb) -> {
          var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
          predicates.add(cb.equal(root.get("user").get("id"), uid));
          if (type != null) predicates.add(cb.equal(root.get("type"), type));
          if (categoryId != null)
            predicates.add(cb.equal(root.get("category").get("id"), categoryId));
          if (from != null)
            predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from.atStartOfDay()));
          if (to != null)
            predicates.add(cb.lessThan(root.get("occurredAt"), to.plusDays(1).atStartOfDay()));
          if (query != null && !query.isBlank()) {
            String term =
                "%"
                    + query
                        .toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_")
                    + "%";
            predicates.add(
                cb.or(
                    cb.like(cb.lower(root.get("merchantName")), term, '\\'),
                    cb.like(cb.lower(root.get("description")), term, '\\')));
          }
          return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    return txs.findAll(
            spec,
            PageRequest.of(
                page, size, Sort.by(Sort.Direction.fromString(direction), sort).and(Sort.by("id"))))
        .map(ViewMapper::transaction);
  }

  public TransactionView create(TransactionRequest request) {
    var user = current.currentUser();
    var category = category(request.categoryId(), user.getId());
    var tx =
        new ExpenseTransaction(
            user,
            request.merchant().trim(),
            request.notes(),
            request.amount(),
            category,
            category.getName(),
            1,
            PaymentStatus.SUCCESSFUL,
            request.occurredAt());
    tx.revise(
        request.merchant().trim(),
        request.notes(),
        request.amount(),
        category,
        request.type(),
        request.occurredAt(),
        request.recurring());
    txs.saveAndFlush(tx);
    publish("TRANSACTION_CREATED", tx);
    return ViewMapper.transaction(tx);
  }

  public TransactionView update(Long id, TransactionRequest request) {
    var tx = owned(id);
    if (tx.isWalletPayment() || tx.isProviderPayment())
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Confirmed payments can only be recategorized");
    tx.revise(
        request.merchant().trim(),
        request.notes(),
        request.amount(),
        category(request.categoryId(), current.currentUserId()),
        request.type(),
        request.occurredAt(),
        request.recurring());
    txs.flush();
    publish("TRANSACTION_UPDATED", tx);
    return ViewMapper.transaction(tx);
  }

  public void delete(Long id) {
    var tx = owned(id);
    if (tx.isWalletPayment() || tx.isProviderPayment())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Confirmed payments cannot be deleted");
    publish("TRANSACTION_DELETED", tx);
    txs.delete(tx);
  }

  private ExpenseTransaction owned(Long id) {
    return txs.findByIdAndUserId(id, current.currentUserId())
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
  }

  private Category category(Long id, Long uid) {
    return categories
        .findById(id)
        .filter(c -> c.getUser().getId().equals(uid))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
  }

  public void publish(String type, ExpenseTransaction tx) {
    events.append(
        type,
        tx.getId(),
        tx.getUser().getId(),
        Map.of(
            "amount",
            tx.getAmount(),
            "type",
            tx.getType().name(),
            "categoryId",
            tx.getCategory().getId(),
            "month",
            YearMonth.from(tx.getOccurredAt()).toString()));
  }
}
