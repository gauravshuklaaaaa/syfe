package com.finance.manager.controller;

import com.finance.manager.dto.request.SavingsGoalRequest;
import com.finance.manager.dto.response.SavingsGoalResponse;
import com.finance.manager.model.User;
import com.finance.manager.service.AuthService;
import com.finance.manager.service.SavingsGoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;
    private final AuthService authService;

    public SavingsGoalController(SavingsGoalService savingsGoalService, AuthService authService) {
        this.savingsGoalService = savingsGoalService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<SavingsGoalResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SavingsGoalRequest request) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(savingsGoalService.create(user, request));
    }

    @GetMapping
    public ResponseEntity<Map<String, List<SavingsGoalResponse>>> getAll(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("goals", savingsGoalService.getAll(user)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavingsGoalResponse> getById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(savingsGoalService.getById(user, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SavingsGoalResponse> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody SavingsGoalRequest request) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(savingsGoalService.update(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        savingsGoalService.delete(user, id);
        return ResponseEntity.ok(Map.of("message", "Goal deleted successfully"));
    }
}
