package com.project.rbac_engine.repository;

import com.project.rbac_engine.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Data access for the Permission entity.
 * findPermissionNamesByUserId is the core query used by CustomPermissionEvaluator
 * to resolve what a user is allowed to do — entirely from the DB, no hardcoding.
 */
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByName(String name);

    boolean existsByName(String name);

    /**
     * Traverses user -> user_roles -> role_permissions -> permissions and returns
     * the distinct permission names for the given user.
     * Called on every @PreAuthorize("hasPermission(...)") evaluation.
     */
    @Query("""
            SELECT DISTINCT p.name
              FROM UserRole ur
              JOIN RolePermission rp ON rp.role = ur.role
              JOIN Permission p      ON p = rp.permission
             WHERE ur.user.id = :userId
            """)
    List<String> findPermissionNamesByUserId(@Param("userId") Long userId);
}

