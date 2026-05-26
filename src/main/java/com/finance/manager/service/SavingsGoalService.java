package com.finance.manager.service;

import com.finance.manager.dto.request.SavingsGoalRequest;
import com.finance.manager.dto.response.SavingsGoalResponse;
import com.finance.manager.exception.BadRequestException;
import com.finance.manager.exception.ForbiddenException;
import com.finance.manager.exception.ResourceNotFoundException;
import com.finance.manager.model.SavingsGoal;
import com.finance.manager.model.TransactionType;
import com.finance.manager.model.User;
import com.finance.manager.repository.SavingsGoalRepository;
import com.finance.manager.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final TransactionRepository transactionRepository;

    public SavingsGoalService(SavingsGoalRepository savingsGoalRepository,
                               TransactionRepository transactionRepository) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public SavingsGoalResponse create(User user, SavingsGoalRequest request) {
        if (!request.getTargetDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Target date must be in the future");
        }
        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setGoalName(request.getGoalName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setTargetDate(request.getTargetDate());
        goal.setStartDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now());
        SavingsGoal saved = savingsGoalRepository.save(goal);
        return buildResponse(saved, user);
    }

    public List<SavingsGoalResponse> getAll(User user) {
        return savingsGoalRepository.findByUser(user).stream()
                .map(g -> buildResponse(g, user))
                .collect(Collectors.toList());
    }

    public SavingsGoalResponse getById(User user, Long id) {
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found: " + id));
        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied to goal: " + id);
        }
        return buildResponse(goal, user);
    }

    @Transactional
    public SavingsGoalResponse update(User user, Long id, SavingsGoalRequest request) {
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found: " + id));
        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied to goal: " + id);
        }
        if (request.getTargetAmount() != null) {
            goal.setTargetAmount(request.getTargetAmount());
        }
        if (request.getTargetDate() != null) {
            if (!request.getTargetDate().isAfter(LocalDate.now())) {
                throw new BadRequestException("Target date must be in the future");
            }
            goal.setTargetDate(request.getTargetDate());
        }
        if (request.getGoalName() != null) {
            goal.setGoalName(request.getGoalName());
        }
        return buildResponse(savingsGoalRepository.save(goal), user);
    }

    @Transactional
    public void delete(User user, Long id) {
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found: " + id));
        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied to goal: " + id);
        }
        savingsGoalRepository.delete(goal);
    }

    private SavingsGoalResponse buildResponse(SavingsGoal goal, User user) {
        BigDecimal income = transactionRepository.sumByUserAndTypeAndDateAfter(
                user, TransactionType.INCOME, goal.getStartDate());
        BigDecimal expense = transactionRepository.sumByUserAndTypeAndDateAfter(
                user, TransactionType.EXPENSE, goal.getStartDate());
        if (income == null) income = BigDecimal.ZERO;
        if (expense == null) expense = BigDecimal.ZERO;

        BigDecimal progress = income.subtract(expense);
        BigDecimal target = goal.getTargetAmount();

        double percentage = target.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : progress.divide(target, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();

        BigDecimal remaining = target.subtract(progress);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;

        SavingsGoalResponse r = new SavingsGoalResponse();
        r.setId(goal.getId());
        r.setGoalName(goal.getGoalName());
        r.setTargetAmount(target);
        r.setTargetDate(goal.getTargetDate());
        r.setStartDate(goal.getStartDate());
        r.setCurrentProgress(progress.setScale(2, RoundingMode.HALF_UP));
        r.setProgressPercentage(percentage);
        r.setRemainingAmount(remaining.setScale(2, RoundingMode.HALF_UP));
        return r;
    }
}
