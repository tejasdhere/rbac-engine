package com.project.rbac_engine.repository;

import com.project.rbac_engine.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PermissionRepositoryTest {

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findPermissionNamesByUserId_returnsDistinctPermissions() {
        User user = entityManager.persist(new User("john", "password", true));

        Role role1 = entityManager.persist(new Role("DEVELOPER"));
        Role role2 = entityManager.persist(new Role("TESTER"));

        Permission p1 = entityManager.persist(new Permission("CODE_READ"));
        Permission p2 = entityManager.persist(new Permission("CODE_WRITE"));

        // role1 has p1, p2
        entityManager.persist(new RolePermission(role1, p1));
        entityManager.persist(new RolePermission(role1, p2));

        // role2 also has p1 (to test DISTINCT)
        entityManager.persist(new RolePermission(role2, p1));

        // user has both roles
        entityManager.persist(new UserRole(user, role1));
        entityManager.persist(new UserRole(user, role2));

        entityManager.flush();
        entityManager.clear();

        List<String> permissionNames = permissionRepository.findPermissionNamesByUserId(user.getId());

        assertEquals(2, permissionNames.size());
        assertTrue(permissionNames.contains("CODE_READ"));
        assertTrue(permissionNames.contains("CODE_WRITE"));
    }

    @Test
    void findPermissionNamesByUserId_whenUserHasNoRoles_returnsEmptyList() {
        User user = entityManager.persist(new User("guest", "password", true));
        entityManager.flush();
        entityManager.clear();

        List<String> permissionNames = permissionRepository.findPermissionNamesByUserId(user.getId());

        assertTrue(permissionNames.isEmpty());
    }
}
