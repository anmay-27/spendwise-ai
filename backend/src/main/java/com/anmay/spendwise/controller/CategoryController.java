package com.anmay.spendwise.controller;

import com.anmay.spendwise.dto.Requests.CreateCategoryRequest;
import com.anmay.spendwise.dto.Responses.CategoryView;
import com.anmay.spendwise.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
  @org.springframework.beans.factory.annotation.Autowired
  private com.anmay.spendwise.security.CurrentUserService currentUser;

  private final CategoryService categoryService;

  public CategoryController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  @GetMapping
  public List<CategoryView> list(@RequestParam(required = false) Long ignoredUserId) {
    return categoryService.list(currentUser.currentUserId());
  }

  @PostMapping
  public CategoryView create(@Valid @RequestBody CreateCategoryRequest request) {
    return categoryService.create(
        new CreateCategoryRequest(currentUser.currentUserId(), request.name(), request.icon()));
  }
}
