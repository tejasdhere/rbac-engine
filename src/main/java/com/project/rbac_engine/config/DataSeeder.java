package com.project.rbac_engine.config;

import com.project.rbac_engine.entity.*;
import com.project.rbac_engine.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds initial bootstrap data on application startup.
 *
 * WHY: A bootstrap administrator and initial permission set must exist in the database
 * so authorized users can interact with the engine. This is purely persisted initial data,
 * NOT hardcoded security logic — all runtime access checks still dynamically evaluate
 * against CustomPermissionEvaluator using database queries.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      RoleRepository roleRepository,
                      PermissionRepository permissionRepository,
                      RolePermissionRepository rolePermissionRepository,
                      UserRoleRepository userRoleRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Seed Permissions
        Permission roleCreate = getOrCreatePermission("ROLE_CREATE");
        Permission permCreate = getOrCreatePermission("PERMISSION_CREATE");
        Permission rolePermAssign = getOrCreatePermission("ROLE_PERMISSION_ASSIGN");
        Permission userRoleAssign = getOrCreatePermission("USER_ROLE_ASSIGN");
        Permission secureDataRead = getOrCreatePermission("SECURE_DATA_READ");

        // 2. Seed Roles
        Role adminRole = getOrCreateRole("ADMIN");
        Role userRole = getOrCreateRole("USER");

        // 3. Seed Role-Permission Mappings
        assignPermissionIfNotPresent(adminRole, roleCreate);
        assignPermissionIfNotPresent(adminRole, permCreate);
        assignPermissionIfNotPresent(adminRole, rolePermAssign);
        assignPermissionIfNotPresent(adminRole, userRoleAssign);
        assignPermissionIfNotPresent(userRole, secureDataRead);

        // 4. Seed Users (passwords BCrypt-encoded)
        User adminUser = getOrCreateUser("admin", "admin123");
        User standardUser = getOrCreateUser("user", "user123");

        // 5. Seed User-Role Mappings
        assignRoleIfNotPresent(adminUser, adminRole);
        assignRoleIfNotPresent(standardUser, userRole);
    }

    private Permission getOrCreatePermission(String name) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(new Permission(name)));
    }

    private Role getOrCreateRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name)));
    }

    private User getOrCreateUser(String username, String rawPassword) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(
                        new User(username, passwordEncoder.encode(rawPassword), true)));
    }

    private void assignPermissionIfNotPresent(Role role, Permission permission) {
        if (!rolePermissionRepository.existsByRoleIdAndPermissionId(role.getId(), permission.getId())) {
            rolePermissionRepository.save(new RolePermission(role, permission));
        }
    }

    private void assignRoleIfNotPresent(User user, Role role) {
        if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            userRoleRepository.save(new UserRole(user, role));
        }
    }
}
