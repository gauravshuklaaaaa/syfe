package com.finance.manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.manager.dto.request.LoginRequest;
import com.finance.manager.dto.request.RegisterRequest;
import com.finance.manager.dto.request.SavingsGoalRequest;
import com.finance.manager.dto.request.TransactionRequest;
import com.finance.manager.repository.SavingsGoalRepository;
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
class SavingsGoalControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired SavingsGoalRepository savingsGoalRepository;
    @Autowired TransactionRepository transactionRepository;

    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        transactionRepository.deleteAll();
        savingsGoalRepository.deleteAll();
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

    private SavingsGoalRequest goalRequest() {
        SavingsGoalRequest r = new SavingsGoalRequest();
        r.setGoalName("Emergency Fund");
        r.setTargetAmount(new BigDecimal("5000.00"));
        r.setTargetDate(LocalDate.now().plusYears(1));
        r.setStartDate(LocalDate.of(2024, 1, 1));
        return r;
    }

    @Test
    void createGoal_success() throws Exception {
        mockMvc.perform(post("/api/goals")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(goalRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.goalName").value("Emergency Fund"))
                .andExpect(jsonPath("$.targetAmount").value(5000.00))
                .andExpect(jsonPath("$.currentProgress").isNumber())
                .andExpect(jsonPath("$.progressPercentage").isNumber())
                .andExpect(jsonPath("$.remainingAmount").isNumber());
    }

    @Test
    void createGoal_pastTargetDate_returnsBadRequest() throws Exception {
        SavingsGoalRequest r = goalRequest();
        r.setTargetDate(LocalDate.now().minusDays(1));
        mockMvc.perform(post("/api/goals")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllGoals_success() throws Exception {
        mockMvc.perform(post("/api/goals")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(goalRequest())));

        mockMvc.perform(get("/api/goals").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goals").isArray())
                .andExpect(jsonPath("$.goals[0].goalName").value("Emergency Fund"));
    }

    @Test
    void getGoalById_success() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/goals")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(goalRequest())))
                .andReturn();
        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/goals/" + id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goalName").value("Emergency Fund"));
    }

    @Test
    void updateGoal_success() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/goals")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(goalRequest())))
                .andReturn();
        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        SavingsGoalRequest update = new SavingsGoalRequest();
        update.setTargetAmount(new BigDecimal("6000.00"));
        update.setTargetDate(LocalDate.now().plusYears(2));

        mockMvc.perform(put("/api/goals/" + id)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetAmount").value(6000.00));
    }

    @Test
    void deleteGoal_success() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/goals")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(goalRequest())))
                .andReturn();
        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/goals/" + id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Goal deleted successfully"));
    }

    @Test
    void goalProgress_reflectsTransactions() throws Exception {
        SavingsGoalRequest goalReq = new SavingsGoalRequest();
        goalReq.setGoalName("Savings Test");
        goalReq.setTargetAmount(new BigDecimal("10000.00"));
        goalReq.setTargetDate(LocalDate.now().plusYears(1));
        goalReq.setStartDate(LocalDate.of(2024, 1, 1));

        MvcResult createResult = mockMvc.perform(post("/api/goals")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(goalReq)))
                .andReturn();
        Long goalId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        TransactionRequest income = new TransactionRequest();
        income.setAmount(new BigDecimal("3000.00"));
        income.setDate(LocalDate.of(2024, 2, 1));
        income.setCategory("Salary");
        mockMvc.perform(post("/api/transactions")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(income)));

        TransactionRequest expense = new TransactionRequest();
        expense.setAmount(new BigDecimal("500.00"));
        expense.setDate(LocalDate.of(2024, 2, 5));
        expense.setCategory("Food");
        mockMvc.perform(post("/api/transactions")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(expense)));

        mockMvc.perform(get("/api/goals/" + goalId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProgress").value(2500.00))
                .andExpect(jsonPath("$.remainingAmount").value(7500.00));
    }

    @Test
    void getGoal_otherUser_returnsForbidden() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/goals")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(goalRequest())))
                .andReturn();
        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

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

        mockMvc.perform(get("/api/goals/" + id).session(session2))
                .andExpect(status().isForbidden());
    }
}
