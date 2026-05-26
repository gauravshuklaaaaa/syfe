package com.finance.manager.service;

import com.finance.manager.dto.request.CategoryRequest;
import com.finance.manager.dto.response.CategoryResponse;
import com.finance.manager.exception.BadRequestException;
import com.finance.manager.exception.ConflictException;
import com.finance.manager.exception.ForbiddenException;
import com.finance.manager.exception.ResourceNotFoundException;
import com.finance.manager.model.Category;
import com.finance.manager.model.User;
import com.finance.manager.repository.CategoryRepository;
import com.finance.manager.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public CategoryService(CategoryRepository categoryRepository, TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<CategoryResponse> getAllCategories(User user) {
        List<Category> defaults = categoryRepository.findByIsDefaultTrue();
        List<Category> custom = categoryRepository.findByUserAndIsDefaultFalse(user);
        List<CategoryResponse> result = new ArrayList<>();
        defaults.forEach(c -> result.add(toResponse(c)));
        custom.forEach(c -> result.add(toResponse(c)));
        return result;
    }

    @Transactional
    public CategoryResponse createCategory(User user, CategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByNameAndIsDefaultTrue(name)) {
            throw new ConflictException("Category already exists: " + name);
        }
        if (categoryRepository.existsByNameAndUser(name, user)) {
            throw new ConflictException("Category already exists: " + name);
        }
        Category category = new Category(name, request.getType(), false, user);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(User user, String name) {
        if (categoryRepository.existsByNameAndIsDefaultTrue(name)) {
            throw new ForbiddenException("Cannot delete default category: " + name);
        }
        Category category = categoryRepository.findByNameAndUser(name, user)
                .orElseThrow(() -> {
                    if (categoryRepository.findByIsDefaultTrue().stream()
                            .noneMatch(c -> c.getName().equals(name))) {
                        return new ResourceNotFoundException("Category not found: " + name);
                    }
                    return new ForbiddenException("Cannot delete another user's category");
                });
        if (transactionRepository.existsByCategory(category)) {
            throw new BadRequestException("Cannot delete category that is in use by transactions");
        }
        categoryRepository.delete(category);
    }

    public Category resolveCategory(User user, String name) {
        return categoryRepository.findByNameAndIsDefaultTrue(name)
                .orElseGet(() -> categoryRepository.findByNameAndUser(name, user)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + name)));
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getName(), c.getType(), !c.isDefault());
    }
}
