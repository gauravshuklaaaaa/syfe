package com.finance.manager.controller;

import com.finance.manager.dto.request.TransactionRequest;
import com.finance.manager.dto.response.TransactionResponse;
import com.finance.manager.model.TransactionType;
import com.finance.manager.model.User;
import com.finance.manager.service.AuthService;
import com.finance.manager.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AuthService authService;

    public TransactionController(TransactionService transactionService, AuthService authService) {
        this.transactionService = transactionService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransactionRequest request) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        TransactionResponse response = transactionService.create(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Map<String, List<TransactionResponse>>> getAll(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TransactionType type) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        List<TransactionResponse> transactions = transactionService.getAll(user, startDate, endDate, categoryId, type);
        return ResponseEntity.ok(Map.of("transactions", transactions));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody TransactionRequest request) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        TransactionResponse response = transactionService.update(user, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        transactionService.delete(user, id);
        return ResponseEntity.ok(Map.of("message", "Transaction deleted successfully"));
    }
}
