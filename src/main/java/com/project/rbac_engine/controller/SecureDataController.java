package com.project.rbac_engine.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller exposing protected business resource endpoint.
 * Requires the SECURE_DATA_READ permission.
 */
@RestController
@RequestMapping("/secure-data")
public class SecureDataController {

    @GetMapping
    @PreAuthorize("hasPermission(null, 'SECURE_DATA_READ')")
    public ResponseEntity<Map<String, String>> getSecureData() {
        return ResponseEntity.ok(Map.of("message", "Access granted to secure data"));
    }
}
