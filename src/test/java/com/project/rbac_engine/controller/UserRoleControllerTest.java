package com.project.rbac_engine.controller;

import com.project.rbac_engine.exception.DuplicateResourceException;
import com.project.rbac_engine.exception.ResourceNotFoundException;
import com.project.rbac_engine.security.CustomPermissionEvaluator;
import com.project.rbac_engine.service.UserRoleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRoleService userRoleService;

    @MockitoBean
    private CustomPermissionEvaluator permissionEvaluator;

    @Test
    void assignRole_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/users/1/roles/2"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "charlie")
    void assignRole_forbidden_returns403() throws Exception {
        when(permissionEvaluator.hasPermission(any(), isNull(), eq("USER_ROLE_ASSIGN"))).thenReturn(false);

        mockMvc.perform(post("/users/1/roles/2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @WithMockUser(username = "admin")
    void assignRole_success_returns201() throws Exception {
        when(permissionEvaluator.hasPermission(any(), isNull(), eq("USER_ROLE_ASSIGN"))).thenReturn(true);
        doNothing().when(userRoleService).assignRole(1L, 2L);

        mockMvc.perform(post("/users/1/roles/2"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Role assigned to user successfully"));
    }

    @Test
    @WithMockUser(username = "admin")
    void assignRole_userNotFound_returns404() throws Exception {
        when(permissionEvaluator.hasPermission(any(), isNull(), eq("USER_ROLE_ASSIGN"))).thenReturn(true);
        doThrow(new ResourceNotFoundException("User not found with id: 99"))
                .when(userRoleService).assignRole(99L, 2L);

        mockMvc.perform(post("/users/99/roles/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "admin")
    void assignRole_duplicate_returns409() throws Exception {
        when(permissionEvaluator.hasPermission(any(), isNull(), eq("USER_ROLE_ASSIGN"))).thenReturn(true);
        doThrow(new DuplicateResourceException("Role 2 is already assigned to User 1"))
                .when(userRoleService).assignRole(1L, 2L);

        mockMvc.perform(post("/users/1/roles/2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
