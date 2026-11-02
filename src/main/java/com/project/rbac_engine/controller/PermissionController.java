package com.project.rbac_engine.controller;

import com.project.rbac_engine.dto.PermissionRequest;
import com.project.rbac_engine.dto.PermissionResponse;
import com.project.rbac_engine.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing permission creation endpoint.
 * Secured strictly via permission evaluation with no hardcoded role checks.
 */
@RestController
@RequestMapping("/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'PERMISSION_CREATE')")
    public ResponseEntity<PermissionResponse> createPermission(@Valid @RequestBody PermissionRequest request) {
        PermissionResponse response = permissionService.createPermission(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
