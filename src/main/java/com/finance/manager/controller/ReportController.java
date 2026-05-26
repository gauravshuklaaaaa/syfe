package com.finance.manager.controller;

import com.finance.manager.dto.response.MonthlyReportResponse;
import com.finance.manager.dto.response.YearlyReportResponse;
import com.finance.manager.model.User;
import com.finance.manager.service.AuthService;
import com.finance.manager.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final AuthService authService;

    public ReportController(ReportService reportService, AuthService authService) {
        this.reportService = reportService;
        this.authService = authService;
    }

    @GetMapping("/monthly/{year}/{month}")
    public ResponseEntity<MonthlyReportResponse> monthly(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable int year,
            @PathVariable int month) {
        if (month < 1 || month > 12) {
            throw new com.finance.manager.exception.BadRequestException("Month must be between 1 and 12");
        }
        User user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(reportService.getMonthlyReport(user, year, month));
    }

    @GetMapping("/yearly/{year}")
    public ResponseEntity<YearlyReportResponse> yearly(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable int year) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(reportService.getYearlyReport(user, year));
    }
}
