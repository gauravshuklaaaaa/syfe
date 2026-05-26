package com.finance.manager.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public class YearlyReportResponse {

    private int year;
    private Map<String, BigDecimal> totalIncome;
    private Map<String, BigDecimal> totalExpenses;
    private BigDecimal netSavings;

    public YearlyReportResponse() {}

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public Map<String, BigDecimal> getTotalIncome() { return totalIncome; }
    public void setTotalIncome(Map<String, BigDecimal> totalIncome) { this.totalIncome = totalIncome; }

    public Map<String, BigDecimal> getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(Map<String, BigDecimal> totalExpenses) { this.totalExpenses = totalExpenses; }

    public BigDecimal getNetSavings() { return netSavings; }
    public void setNetSavings(BigDecimal netSavings) { this.netSavings = netSavings; }
}
