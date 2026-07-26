package com.anmay.spendwise.controller;

import com.anmay.spendwise.dto.Requests.CreateCategoryRequest;
import com.anmay.spendwise.dto.Responses.CategoryView;
import com.anmay.spendwise.security.CurrentUserService;
import com.anmay.spendwise.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;
    private final CurrentUserService currentUserService;

    public CategoryController(CategoryService categoryService,
                              CurrentUserService currentUserService) {
        this.categoryService = categoryService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<CategoryView> list() {
        return categoryService.list(currentUserService.currentUserId());
    }

    @PostMapping
    public CategoryView create(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.create(currentUserService.currentUserId(), request);
    }
}
