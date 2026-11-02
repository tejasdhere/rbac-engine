package com.project.rbac_engine.controller;

import com.project.rbac_engine.dto.PermissionRequest;
import com.project.rbac_engine.dto.PermissionResponse;
import com.project.rbac_engine.exception.DuplicateResourceException;
import com.project.rbac_engine.security.CustomPermissionEvaluator;
import com.project.rbac_engine.service.PermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PermissionService permissionService;

    @MockitoBean
    private CustomPermissionEvaluator permissionEvaluator;

    @Test
    void createPermission_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"USER_EXPORT\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "bob")
    void createPermission_forbidden_returns403() throws Exception {
        when(permissionEvaluator.hasPermission(any(), isNull(), eq("PERMISSION_CREATE"))).thenReturn(false);

        mockMvc.perform(post("/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"USER_EXPORT\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @WithMockUser(username = "admin")
    void createPermission_success_returns201() throws Exception {
        when(permissionEvaluator.hasPermission(any(), isNull(), eq("PERMISSION_CREATE"))).thenReturn(true);
        when(permissionService.createPermission(any(PermissionRequest.class)))
                .thenReturn(new PermissionResponse(1L, "USER_EXPORT"));

        mockMvc.perform(post("/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"USER_EXPORT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("USER_EXPORT"));
    }

    @Test
    @WithMockUser(username = "admin")
    void createPermission_blankName_returns400() throws Exception {
        when(permissionEvaluator.hasPermission(any(), isNull(), eq("PERMISSION_CREATE"))).thenReturn(true);

        mockMvc.perform(post("/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin")
    void createPermission_duplicate_returns409() throws Exception {
        when(permissionEvaluator.hasPermission(any(), isNull(), eq("PERMISSION_CREATE"))).thenReturn(true);
        when(permissionService.createPermission(any(PermissionRequest.class)))
                .thenThrow(new DuplicateResourceException("Permission already exists with name: USER_EXPORT"));

        mockMvc.perform(post("/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"USER_EXPORT\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
