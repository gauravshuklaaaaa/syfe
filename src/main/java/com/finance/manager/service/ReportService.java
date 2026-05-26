package com.finance.manager.service;

import com.finance.manager.dto.response.MonthlyReportResponse;
import com.finance.manager.dto.response.YearlyReportResponse;
import com.finance.manager.model.Transaction;
import com.finance.manager.model.TransactionType;
import com.finance.manager.model.User;
import com.finance.manager.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;

    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public MonthlyReportResponse getMonthlyReport(User user, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        List<Transaction> transactions = transactionRepository.findByUserAndDateBetween(user, start, end);

        Map<String, BigDecimal> income = new HashMap<>();
        Map<String, BigDecimal> expenses = new HashMap<>();

        for (Transaction t : transactions) {
            String cat = t.getCategory().getName();
            if (t.getType() == TransactionType.INCOME) {
                income.merge(cat, t.getAmount(), BigDecimal::add);
            } else {
                expenses.merge(cat, t.getAmount(), BigDecimal::add);
            }
        }

        BigDecimal totalIncome = income.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = expenses.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        MonthlyReportResponse r = new MonthlyReportResponse();
        r.setMonth(month);
        r.setYear(year);
        r.setTotalIncome(income);
        r.setTotalExpenses(expenses);
        r.setNetSavings(totalIncome.subtract(totalExpense));
        return r;
    }

    public YearlyReportResponse getYearlyReport(User user, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        List<Transaction> transactions = transactionRepository.findByUserAndDateBetween(user, start, end);

        Map<String, BigDecimal> income = new HashMap<>();
        Map<String, BigDecimal> expenses = new HashMap<>();

        for (Transaction t : transactions) {
            String cat = t.getCategory().getName();
            if (t.getType() == TransactionType.INCOME) {
                income.merge(cat, t.getAmount(), BigDecimal::add);
            } else {
                expenses.merge(cat, t.getAmount(), BigDecimal::add);
            }
        }

        BigDecimal totalIncome = income.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = expenses.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        YearlyReportResponse r = new YearlyReportResponse();
        r.setYear(year);
        r.setTotalIncome(income);
        r.setTotalExpenses(expenses);
        r.setNetSavings(totalIncome.subtract(totalExpense));
        return r;
    }
}
