package com.project.rbac_engine.repository;

import com.project.rbac_engine.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access for the Role entity.
 * existsByName is used by RoleService to detect duplicates before saving.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);
}

