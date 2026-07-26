package com.anmay.spendwise.service;

import com.anmay.spendwise.dto.Requests.CreateCategoryRequest;
import com.anmay.spendwise.dto.Responses.CategoryView;
import com.anmay.spendwise.entity.AppUser;
import com.anmay.spendwise.entity.Category;
import com.anmay.spendwise.repository.AppUserRepository;
import com.anmay.spendwise.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final AppUserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository, AppUserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryView> list(Long userId) {
        return categoryRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(this::view).toList();
    }

    @Transactional
    public CategoryView create(CreateCategoryRequest request) {
        AppUser user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        categoryRepository.findByUserIdAndNameIgnoreCase(request.userId(), request.name())
                .ifPresent(existing -> { throw new IllegalArgumentException("Category already exists"); });
        String icon = request.icon() == null || request.icon().isBlank() ? "🏷️" : request.icon().trim();
        Category category = categoryRepository.save(
                new Category(user, request.name().trim(), icon, false));
        return view(category);
    }

    private CategoryView view(Category category) {
        return new CategoryView(category.getId(), category.getName(), category.getIcon(), category.isSystemDefined());
    }
}
