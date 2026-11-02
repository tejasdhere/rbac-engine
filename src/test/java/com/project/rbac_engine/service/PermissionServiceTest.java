package com.project.rbac_engine.service;

import com.project.rbac_engine.dto.PermissionRequest;
import com.project.rbac_engine.dto.PermissionResponse;
import com.project.rbac_engine.entity.Permission;
import com.project.rbac_engine.exception.DuplicateResourceException;
import com.project.rbac_engine.repository.PermissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionService permissionService;

    @Test
    void createPermission_success() {
        PermissionRequest request = new PermissionRequest("NEW_PERM");
        Permission saved = new Permission("NEW_PERM");
        saved.setId(5L);

        when(permissionRepository.existsByName("NEW_PERM")).thenReturn(false);
        when(permissionRepository.save(any(Permission.class))).thenReturn(saved);

        PermissionResponse response = permissionService.createPermission(request);

        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals("NEW_PERM", response.getName());
        verify(permissionRepository).save(any(Permission.class));
    }

    @Test
    void createPermission_duplicate_throwsDuplicateResourceException() {
        PermissionRequest request = new PermissionRequest("ROLE_CREATE");
        when(permissionRepository.existsByName("ROLE_CREATE")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> permissionService.createPermission(request));
        verify(permissionRepository, never()).save(any(Permission.class));
    }
}

