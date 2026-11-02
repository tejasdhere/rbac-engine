package com.project.rbac_engine.controller;

import com.project.rbac_engine.security.CustomPermissionEvaluator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecureDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomPermissionEvaluator permissionEvaluator;

    @Test
    void getSecureData_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/secure-data"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "guest")
    void getSecureData_forbidden_returns403() throws Exception {
        when(permissionEvaluator.hasPermission(any(), isNull(), eq("SECURE_DATA_READ"))).thenReturn(false);

        mockMvc.perform(get("/secure-data"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @WithMockUser(username = "user")
    void getSecureData_success_returns200() throws Exception {
        when(permissionEvaluator.hasPermission(any(), isNull(), eq("SECURE_DATA_READ"))).thenReturn(true);

        mockMvc.perform(get("/secure-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Access granted to secure data"));
    }
}
