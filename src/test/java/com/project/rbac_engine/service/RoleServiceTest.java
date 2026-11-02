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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @InjectMocks
    private RoleService roleService;

    @Test
    void createRole_success() {
        RoleRequest request = new RoleRequest("MANAGER");
        Role saved = new Role("MANAGER");
        saved.setId(10L);

        when(roleRepository.existsByName("MANAGER")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(saved);

        RoleResponse response = roleService.createRole(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("MANAGER", response.getName());
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void createRole_duplicate_throwsDuplicateResourceException() {
        RoleRequest request = new RoleRequest("ADMIN");
        when(roleRepository.existsByName("ADMIN")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> roleService.createRole(request));
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void assignPermission_success() {
        Role role = new Role("ADMIN");
        role.setId(1L);
        Permission permission = new Permission("ROLE_CREATE");
        permission.setId(2L);

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findById(2L)).thenReturn(Optional.of(permission));
        when(rolePermissionRepository.existsByRoleIdAndPermissionId(1L, 2L)).thenReturn(false);

        roleService.assignPermission(1L, 2L);

        verify(rolePermissionRepository).save(any(RolePermission.class));
    }

    @Test
    void assignPermission_roleNotFound_throwsResourceNotFoundException() {
        when(roleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roleService.assignPermission(1L, 2L));
        verify(rolePermissionRepository, never()).save(any(RolePermission.class));
    }

    @Test
    void assignPermission_permissionNotFound_throwsResourceNotFoundException() {
        Role role = new Role("ADMIN");
        role.setId(1L);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roleService.assignPermission(1L, 2L));
        verify(rolePermissionRepository, never()).save(any(RolePermission.class));
    }

    @Test
    void assignPermission_alreadyAssigned_throwsDuplicateResourceException() {
        Role role = new Role("ADMIN");
        role.setId(1L);
        Permission permission = new Permission("ROLE_CREATE");
        permission.setId(2L);

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findById(2L)).thenReturn(Optional.of(permission));
        when(rolePermissionRepository.existsByRoleIdAndPermissionId(1L, 2L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> roleService.assignPermission(1L, 2L));
        verify(rolePermissionRepository, never()).save(any(RolePermission.class));
    }
}

