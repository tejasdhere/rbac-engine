package com.project.rbac_engine.controller;

import com.project.rbac_engine.dto.RoleRequest;
import com.project.rbac_engine.dto.RoleResponse;
import com.project.rbac_engine.exception.DuplicateResourceException;
import com.project.rbac_engine.exception.ResourceNotFoundException;
import com.project.rbac_engine.security.CustomPermissionEvaluator;
import com.project.rbac_engine.service.RoleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private CustomPermissionEvaluator permissionEvaluator;

    // ── POST /roles ─────────────────────────────────────────────────────────────

    @Test
    void createRole_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"MANAGER\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @WithMockUser(username = "alice")
    void createRole_forbidden_returns403() throws Exception {
        when(permissionEvaluator.hasPermission(any(), isNull(), eq("ROLE_CREATE"))).thenReturn(false);

        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"MANAGER\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @WithMockUser(username = "admin")
    void createRole_success_returns201() throws Exception {
        when(permissionEvaluator.hasPermission(any(), isNull(), eq("ROLE_CREATE"))).thenReturn(true);
        when(roleService.createRole(any(RoleRequest.class))).thenReturn(new RoleResponse(1L, "MANAGER"));

        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"MANAGER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("MANAGER"));
    }

    @Test
    @WithMockUser(username = "admin")
    void createRole_blankName_returns400() throws Exception {
        when(permissionEvaluator.hasPermission(any(), isNull(), eq("ROLE_CREATE"))).thenReturn(true);

        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @WithMockUser(username = "admin")
    void createRole_duplicateName_returns409() throws Exception {
        when(permissionEvaluator.hasPermission(any(), isNull(), eq("ROLE_CREATE"))).thenReturn(true);
        when(roleService.createRole(any(RoleRequest.class)))
                .thenThrow(new DuplicateResourceException("Role already exists with name: ADMIN"));

        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ADMIN\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Role already exists with name: ADMIN"));
    }

    // ── POST /roles/{roleId}/permissions/{permissionId} ─────────────────────────

    @Test
    @WithMockUser(username = "admin")
    void assignPermission_success_returns201() throws Exception {
        when(permissionEvaluator.hasPermission(any(), isNull(), eq("ROLE_PERMISSION_ASSIGN"))).thenReturn(true);
        doNothing().when(roleService).assignPermission(1L, 2L);

        mockMvc.perform(post("/roles/1/permissions/2"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Permission assigned to role successfully"));
    }

    @Test
    @WithMockUser(username = "admin")
    void assignPermission_roleOrPermissionNotFound_returns404() throws Exception {
        when(permissionEvaluator.hasPermission(any(), isNull(), eq("ROLE_PERMISSION_ASSIGN"))).thenReturn(true);
        doThrow(new ResourceNotFoundException("Role not found with id: 99"))
                .when(roleService).assignPermission(99L, 2L);

        mockMvc.perform(post("/roles/99/permissions/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Role not found with id: 99"));
    }

    @Test
    @WithMockUser(username = "admin")
    void assignPermission_duplicate_returns409() throws Exception {
        when(permissionEvaluator.hasPermission(any(), isNull(), eq("ROLE_PERMISSION_ASSIGN"))).thenReturn(true);
        doThrow(new DuplicateResourceException("Permission 2 is already assigned to Role 1"))
                .when(roleService).assignPermission(1L, 2L);

        mockMvc.perform(post("/roles/1/permissions/2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
