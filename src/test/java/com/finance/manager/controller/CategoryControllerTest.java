package com.finance.manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.manager.dto.request.CategoryRequest;
import com.finance.manager.dto.request.LoginRequest;
import com.finance.manager.dto.request.RegisterRequest;
import com.finance.manager.dto.request.TransactionRequest;
import com.finance.manager.model.TransactionType;
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
class CategoryControllerTest {

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

    @Test
    void getCategories_includesDefaults() throws Exception {
        mockMvc.perform(get("/api/categories").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories[?(@.name == 'Salary')].type").value("INCOME"))
                .andExpect(jsonPath("$.categories[?(@.name == 'Food')].type").value("EXPENSE"))
                .andExpect(jsonPath("$.categories[?(@.name == 'Salary')].isCustom").value(false));
    }

    @Test
    void createCustomCategory_success() throws Exception {
        CategoryRequest req = new CategoryRequest();
        req.setName("SideBusinessIncome");
        req.setType(TransactionType.INCOME);

        mockMvc.perform(post("/api/categories")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("SideBusinessIncome"))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.isCustom").value(true));
    }

    @Test
    void createCustomCategory_duplicate_returnsConflict() throws Exception {
        CategoryRequest req = new CategoryRequest();
        req.setName("MyCategory");
        req.setType(TransactionType.EXPENSE);

        mockMvc.perform(post("/api/categories")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/categories")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteCustomCategory_success() throws Exception {
        CategoryRequest req = new CategoryRequest();
        req.setName("TempCategory");
        req.setType(TransactionType.EXPENSE);
        mockMvc.perform(post("/api/categories")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        mockMvc.perform(delete("/api/categories/TempCategory").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category deleted successfully"));
    }

    @Test
    void deleteDefaultCategory_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/categories/Salary").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCategory_inUse_returnsBadRequest() throws Exception {
        CategoryRequest req = new CategoryRequest();
        req.setName("InUseCategory");
        req.setType(TransactionType.EXPENSE);
        mockMvc.perform(post("/api/categories")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        TransactionRequest txReq = new TransactionRequest();
        txReq.setAmount(new BigDecimal("100.00"));
        txReq.setDate(LocalDate.of(2024, 1, 10));
        txReq.setCategory("InUseCategory");
        mockMvc.perform(post("/api/transactions")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(txReq)));

        mockMvc.perform(delete("/api/categories/InUseCategory").session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCategories_withoutSession_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());
    }
}
