package com.finance.manager.controller;

import com.finance.manager.dto.request.CategoryRequest;
import com.finance.manager.dto.response.CategoryResponse;
import com.finance.manager.model.User;
import com.finance.manager.service.AuthService;
import com.finance.manager.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final AuthService authService;

    public CategoryController(CategoryService categoryService, AuthService authService) {
        this.categoryService = categoryService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<Map<String, List<CategoryResponse>>> getAll(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        List<CategoryResponse> categories = categoryService.getAllCategories(user);
        return ResponseEntity.ok(Map.of("categories", categories));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CategoryRequest request) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        CategoryResponse response = categoryService.createCategory(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Map<String, String>> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String name) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        categoryService.deleteCategory(user, name);
        return ResponseEntity.ok(Map.of("message", "Category deleted successfully"));
    }
}
