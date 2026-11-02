package com.project.rbac_engine.service;

import com.project.rbac_engine.dto.PermissionRequest;
import com.project.rbac_engine.dto.PermissionResponse;
import com.project.rbac_engine.entity.Permission;
import com.project.rbac_engine.exception.DuplicateResourceException;
import com.project.rbac_engine.repository.PermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing permission entities.
 * Contains no security/authorization logic; purely business domain operations.
 */
@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    /**
     * Creates a new permission if the name is not already taken.
     */
    @Transactional
    public PermissionResponse createPermission(PermissionRequest request) {
        if (permissionRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Permission already exists with name: " + request.getName());
        }
        Permission saved = permissionRepository.save(new Permission(request.getName()));
        return new PermissionResponse(saved.getId(), saved.getName());
    }
}
