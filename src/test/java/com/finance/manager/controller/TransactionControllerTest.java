package com.finance.manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.manager.dto.request.LoginRequest;
import com.finance.manager.dto.request.RegisterRequest;
import com.finance.manager.dto.request.TransactionRequest;
import com.finance.manager.model.User;
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
class TransactionControllerTest {

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
    }

    private TransactionRequest salaryRequest() {
        TransactionRequest r = new TransactionRequest();
        r.setAmount(new BigDecimal("50000.00"));
        r.setDate(LocalDate.of(2024, 1, 15));
        r.setCategory("Salary");
        r.setDescription("January Salary");
        return r;
    }

    @Test
    void createTransaction_success() throws Exception {
        mockMvc.perform(post("/api/transactions")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(salaryRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.amount").value(50000.00))
                .andExpect(jsonPath("$.category").value("Salary"))
                .andExpect(jsonPath("$.type").value("INCOME"));
    }

    @Test
    void createTransaction_futureDate_returnsBadRequest() throws Exception {
        TransactionRequest r = salaryRequest();
        r.setDate(LocalDate.now().plusDays(1));
        mockMvc.perform(post("/api/transactions")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_invalidCategory_returnsBadRequest() throws Exception {
        TransactionRequest r = salaryRequest();
        r.setCategory("NonExistentCategory");
        mockMvc.perform(post("/api/transactions")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTransactions_success() throws Exception {
        mockMvc.perform(post("/api/transactions")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(salaryRequest())));

        mockMvc.perform(get("/api/transactions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.transactions[0].category").value("Salary"));
    }

    @Test
    void updateTransaction_success() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/transactions")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(salaryRequest())))
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        TransactionRequest update = new TransactionRequest();
        update.setAmount(new BigDecimal("60000.00"));
        update.setDescription("Updated January Salary");

        mockMvc.perform(put("/api/transactions/" + id)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(60000.00))
                .andExpect(jsonPath("$.description").value("Updated January Salary"))
                .andExpect(jsonPath("$.date").value("2024-01-15"));
    }

    @Test
    void deleteTransaction_success() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/transactions")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(salaryRequest())))
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/transactions/" + id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction deleted successfully"));
    }

    @Test
    void deleteTransaction_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/transactions/9999").session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTransactions_withoutSession_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dataIsolation_otherUserCannotSeeTransactions() throws Exception {
        mockMvc.perform(post("/api/transactions")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(salaryRequest())));

        RegisterRequest reg2 = new RegisterRequest();
        reg2.setUsername("jane@example.com");
        reg2.setPassword("password123");
        reg2.setFullName("Jane Doe");
        reg2.setPhoneNumber("+9876543210");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg2)));

        LoginRequest login2 = new LoginRequest();
        login2.setUsername("jane@example.com");
        login2.setPassword("password123");
        MvcResult result2 = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login2)))
                .andReturn();
        MockHttpSession session2 = (MockHttpSession) result2.getRequest().getSession();

        mockMvc.perform(get("/api/transactions").session(session2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions").isEmpty());
    }
}
