package com.project.rbac_engine.security;

import com.project.rbac_engine.entity.Permission;
import com.project.rbac_engine.entity.Role;
import com.project.rbac_engine.repository.PermissionRepository;
import com.project.rbac_engine.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test proving that RBAC permissions are evaluated dynamically
 * at runtime directly from the database without server restart or hardcoded roles.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DynamicRbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Test
    void permissionsAreDynamic_userGetsAccessAfterAdminAssignsPermission() throws Exception {
        // Step 1: Standard 'user' attempts to create a role -> Expect 403 Forbidden
        mockMvc.perform(post("/roles")
                        .with(httpBasic("user", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"HR_MANAGER\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));

        // Step 2: 'admin' grants ROLE_CREATE permission to the USER role via API
        Role userRole = roleRepository.findByName("USER").orElseThrow();
        Permission roleCreatePerm = permissionRepository.findByName("ROLE_CREATE").orElseThrow();

        mockMvc.perform(post("/roles/" + userRole.getId() + "/permissions/" + roleCreatePerm.getId())
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Permission assigned to role successfully"));

        // Step 3: Same 'user' tries to create a role again -> Expect 201 Created (immediate runtime effect!)
        mockMvc.perform(post("/roles")
                        .with(httpBasic("user", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"HR_MANAGER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("HR_MANAGER"));
    }
}
