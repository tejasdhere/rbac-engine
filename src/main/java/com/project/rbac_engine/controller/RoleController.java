package com.project.rbac_engine.controller;

import com.project.rbac_engine.dto.RoleRequest;
import com.project.rbac_engine.dto.RoleResponse;
import com.project.rbac_engine.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller exposing role creation and role-permission assignment endpoints.
 * Secured strictly via permission evaluation with no hardcoded role checks.
 */
@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'ROLE_CREATE')")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        RoleResponse response = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{roleId}/permissions/{permissionId}")
    @PreAuthorize("hasPermission(null, 'ROLE_PERMISSION_ASSIGN')")
    public ResponseEntity<Map<String, String>> assignPermission(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {
        roleService.assignPermission(roleId, permissionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Permission assigned to role successfully"));
    }
}
