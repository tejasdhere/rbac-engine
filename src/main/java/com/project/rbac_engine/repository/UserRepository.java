package com.project.rbac_engine.repository;

import com.project.rbac_engine.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access for the User entity.
 * findByUsername is used by CustomUserDetailsService during authentication.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    // Used by DataSeeder to avoid inserting duplicate seed users.
    boolean existsByUsername(String username);
}

