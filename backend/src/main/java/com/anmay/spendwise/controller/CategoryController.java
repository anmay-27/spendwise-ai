package com.anmay.spendwise.controller;

import com.anmay.spendwise.dto.Requests.CreateCategoryRequest;
import com.anmay.spendwise.dto.Responses.CategoryView;
import com.anmay.spendwise.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;
    public CategoryController(CategoryService categoryService) { this.categoryService = categoryService; }

    @GetMapping
    public List<CategoryView> list(@RequestParam(defaultValue = "1") Long userId) {
        return categoryService.list(userId);
    }

    @PostMapping
    public CategoryView create(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.create(request);
    }
}
