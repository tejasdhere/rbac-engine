package com.project.rbac_engine.service;

import com.project.rbac_engine.entity.Role;
import com.project.rbac_engine.entity.User;
import com.project.rbac_engine.entity.UserRole;
import com.project.rbac_engine.exception.DuplicateResourceException;
import com.project.rbac_engine.exception.ResourceNotFoundException;
import com.project.rbac_engine.repository.RoleRepository;
import com.project.rbac_engine.repository.UserRepository;
import com.project.rbac_engine.repository.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user-role assignments.
 * Contains no security/authorization logic; purely business domain operations.
 */
@Service
public class UserRoleService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public UserRoleService(UserRepository userRepository,
                           RoleRepository roleRepository,
                           UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    /**
     * Associates a role to a user.
     */
    @Transactional
    public void assignRole(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        if (userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            throw new DuplicateResourceException(
                    "Role " + roleId + " is already assigned to User " + userId);
        }

        userRoleRepository.save(new UserRole(user, role));
    }
}
