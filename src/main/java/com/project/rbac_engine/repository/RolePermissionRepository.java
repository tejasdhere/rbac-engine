package com.project.rbac_engine.repository;

import com.project.rbac_engine.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for the RolePermission join table.
 * existsByRoleIdAndPermissionId prevents duplicate role-permission assignments.
 */
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);
}

