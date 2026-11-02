package com.project.rbac_engine.repository;

import com.project.rbac_engine.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for the UserRole join table.
 * existsByUserIdAndRoleId prevents duplicate user-role assignments.
 */
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    boolean existsByUserIdAndRoleId(Long userId, Long roleId);
}

