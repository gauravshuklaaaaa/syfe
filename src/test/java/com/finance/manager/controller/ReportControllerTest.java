package com.finance.manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.manager.dto.request.LoginRequest;
import com.finance.manager.dto.request.RegisterRequest;
import com.finance.manager.dto.request.TransactionRequest;
import com.finance.manager.repository.TransactionRepository;
import com.finance.manager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReportControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired TransactionRepository transactionRepository;

    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        transactionRepository.deleteAll();
        userRepository.deleteAll();

        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("john@example.com");
        reg.setPassword("password123");
        reg.setFullName("John Doe");
        reg.setPhoneNumber("+1234567890");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        LoginRequest login = new LoginRequest();
        login.setUsername("john@example.com");
        login.setPassword("password123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andReturn();
        session = (MockHttpSession) result.getRequest().getSession();

        TransactionRequest income = new TransactionRequest();
        income.setAmount(new BigDecimal("3000.00"));
        income.setDate(LocalDate.of(2024, 1, 15));
        income.setCategory("Salary");
        income.setDescription("January Salary");
        mockMvc.perform(post("/api/transactions")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(income)));

        TransactionRequest expense = new TransactionRequest();
        expense.setAmount(new BigDecimal("500.00"));
        expense.setDate(LocalDate.of(2024, 1, 20));
        expense.setCategory("Food");
        mockMvc.perform(post("/api/transactions")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(expense)));
    }

    @Test
    void monthlyReport_success() throws Exception {
        mockMvc.perform(get("/api/reports/monthly/2024/1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(1))
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.totalIncome.Salary").value(3000.00))
                .andExpect(jsonPath("$.totalExpenses.Food").value(500.00))
                .andExpect(jsonPath("$.netSavings").value(2500.00));
    }

    @Test
    void yearlyReport_success() throws Exception {
        mockMvc.perform(get("/api/reports/yearly/2024").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.totalIncome.Salary").value(3000.00))
                .andExpect(jsonPath("$.totalExpenses.Food").value(500.00))
                .andExpect(jsonPath("$.netSavings").value(2500.00));
    }

    @Test
    void monthlyReport_withoutSession_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/reports/monthly/2024/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void monthlyReport_invalidMonth_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reports/monthly/2024/13").session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void monthlyReport_emptyMonth_returnsZeros() throws Exception {
        mockMvc.perform(get("/api/reports/monthly/2024/6").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netSavings").value(0));
    }
}
