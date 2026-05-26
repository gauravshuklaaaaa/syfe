package com.finance.manager.service;

import com.finance.manager.dto.request.TransactionRequest;
import com.finance.manager.dto.response.TransactionResponse;
import com.finance.manager.exception.BadRequestException;
import com.finance.manager.exception.ResourceNotFoundException;
import com.finance.manager.model.Category;
import com.finance.manager.model.Transaction;
import com.finance.manager.model.TransactionType;
import com.finance.manager.model.User;
import com.finance.manager.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    public TransactionService(TransactionRepository transactionRepository, CategoryService categoryService) {
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
    }

    @Transactional
    public TransactionResponse create(User user, TransactionRequest request) {
        if (request.getDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Transaction date cannot be in the future");
        }
        Category category = categoryService.resolveCategory(user, request.getCategory());
        Transaction t = new Transaction();
        t.setUser(user);
        t.setAmount(request.getAmount());
        t.setDate(request.getDate());
        t.setCategory(category);
        t.setDescription(request.getDescription());
        t.setType(category.getType());
        return toResponse(transactionRepository.save(t));
    }

    public List<TransactionResponse> getAll(User user, LocalDate startDate, LocalDate endDate,
                                             Long categoryId, TransactionType type) {
        Category categoryFilter = null;
        if (categoryId != null) {
            categoryFilter = transactionRepository.findById(categoryId)
                    .map(Transaction::getCategory)
                    .orElse(null);
        }
        return transactionRepository.findFiltered(user, startDate, endDate, categoryFilter, type)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public TransactionResponse getById(User user, Long id) {
        Transaction t = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
        return toResponse(t);
    }

    @Transactional
    public TransactionResponse update(User user, Long id, TransactionRequest request) {
        Transaction t = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
        if (request.getAmount() != null) {
            t.setAmount(request.getAmount());
        }
        if (request.getCategory() != null) {
            Category category = categoryService.resolveCategory(user, request.getCategory());
            t.setCategory(category);
            t.setType(category.getType());
        }
        if (request.getDescription() != null) {
            t.setDescription(request.getDescription());
        }
        return toResponse(transactionRepository.save(t));
    }

    @Transactional
    public void delete(User user, Long id) {
        Transaction t = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
        transactionRepository.delete(t);
    }

    public TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(t.getId(), t.getAmount(), t.getDate(),
                t.getCategory().getName(), t.getDescription(), t.getType());
    }
}
