package com.project.rbac_engine.controller;

import com.project.rbac_engine.service.UserRoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller exposing user-role assignment endpoint.
 * Secured strictly via permission evaluation with no hardcoded role checks.
 */
@RestController
@RequestMapping("/users")
public class UserRoleController {

    private final UserRoleService userRoleService;

    public UserRoleController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    @PostMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasPermission(null, 'USER_ROLE_ASSIGN')")
    public ResponseEntity<Map<String, String>> assignRole(
            @PathVariable Long userId,
            @PathVariable Long roleId) {
        userRoleService.assignRole(userId, roleId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Role assigned to user successfully"));
    }
}
