package com.project.rbac_engine.service;

import com.project.rbac_engine.dto.RoleRequest;
import com.project.rbac_engine.dto.RoleResponse;
import com.project.rbac_engine.entity.Permission;
import com.project.rbac_engine.entity.Role;
import com.project.rbac_engine.entity.RolePermission;
import com.project.rbac_engine.exception.DuplicateResourceException;
import com.project.rbac_engine.exception.ResourceNotFoundException;
import com.project.rbac_engine.repository.PermissionRepository;
import com.project.rbac_engine.repository.RolePermissionRepository;
import com.project.rbac_engine.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing roles and role-permission associations.
 * Contains no security/authorization logic; purely business domain operations.
 */
@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public RoleService(RoleRepository roleRepository,
                       PermissionRepository permissionRepository,
                       RolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    /**
     * Creates a new role if the name is not already taken.
     */
    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Role already exists with name: " + request.getName());
        }
        Role saved = roleRepository.save(new Role(request.getName()));
        return new RoleResponse(saved.getId(), saved.getName());
    }

    /**
     * Associates a permission to a role.
     */
    @Transactional
    public void assignPermission(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + permissionId));

        if (rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            throw new DuplicateResourceException(
                    "Permission " + permissionId + " is already assigned to Role " + roleId);
        }

        rolePermissionRepository.save(new RolePermission(role, permission));
    }
}
