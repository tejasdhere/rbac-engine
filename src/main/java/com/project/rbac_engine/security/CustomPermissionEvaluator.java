package com.project.rbac_engine.security;

import com.project.rbac_engine.entity.User;
import com.project.rbac_engine.repository.PermissionRepository;
import com.project.rbac_engine.repository.UserRepository;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

/**
 * The ONLY class in the application that makes authorization decisions.
 *
 * Every @PreAuthorize("hasPermission(null, 'SOME_PERMISSION')") annotation on a
 * controller or service method ultimately calls hasPermission() here.
 * The decision is made by reading the database — no role names, no hardcoded maps.
 *
 * Flow: authenticated username → User row → UserRole rows → RolePermission rows
 *       → Permission names → contains(requested permission)?
 */
@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    public CustomPermissionEvaluator(UserRepository userRepository,
                                     PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
    }

    /**
     * Called by Spring Security for every @PreAuthorize("hasPermission(target, permission)").
     *
     * @param authentication      the current principal (from the Security context)
     * @param targetDomainObject  the first argument to hasPermission(); we use null for global checks
     * @param permission          the permission string, e.g. "ROLE_CREATE"
     */
    @Override
    public boolean hasPermission(Authentication authentication,
                                 Object targetDomainObject,
                                 Object permission) {

        // Reject immediately if there is no valid, authenticated principal.
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Permission must be a String; anything else is a programming error — deny safely.
        if (!(permission instanceof String permissionName)) {
            return false;
        }

        // Look up the user in the DB. If the principal is somehow not in the DB, deny.
        return userRepository.findByUsername(authentication.getName())
                .map(user -> hasPermissionInDb(user, permissionName))
                .orElse(false);
    }

    /**
     * Overload called by Spring Security when hasPermission receives a target id + type.
     * We don't use domain-object–level security in this project, so we delegate to the
     * main method with targetId standing in as the target object.
     */
    @Override
    public boolean hasPermission(Authentication authentication,
                                 Serializable targetId,
                                 String targetType,
                                 Object permission) {
        // Delegate — keeps all decision logic in one place.
        return hasPermission(authentication, targetId, permission);
    }

    /**
     * Queries the DB for all permission names the user currently holds and checks membership.
     * A fresh query on every call means DB changes take effect immediately — no stale cache.
     */
    private boolean hasPermissionInDb(User user, String permissionName) {
        List<String> granted = permissionRepository.findPermissionNamesByUserId(user.getId());
        return granted.contains(permissionName);
    }
}
