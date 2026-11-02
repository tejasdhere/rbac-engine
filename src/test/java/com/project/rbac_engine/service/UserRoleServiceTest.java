package com.project.rbac_engine.service;

import com.project.rbac_engine.entity.Role;
import com.project.rbac_engine.entity.User;
import com.project.rbac_engine.entity.UserRole;
import com.project.rbac_engine.exception.DuplicateResourceException;
import com.project.rbac_engine.exception.ResourceNotFoundException;
import com.project.rbac_engine.repository.RoleRepository;
import com.project.rbac_engine.repository.UserRepository;
import com.project.rbac_engine.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private UserRoleService userRoleService;

    @Test
    void assignRole_success() {
        User user = new User("alice", "pass", true);
        user.setId(1L);
        Role role = new Role("ADMIN");
        role.setId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
        when(userRoleRepository.existsByUserIdAndRoleId(1L, 2L)).thenReturn(false);

        userRoleService.assignRole(1L, 2L);

        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    void assignRole_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userRoleService.assignRole(1L, 2L));
        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    void assignRole_roleNotFound_throwsResourceNotFoundException() {
        User user = new User("alice", "pass", true);
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userRoleService.assignRole(1L, 2L));
        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    void assignRole_alreadyAssigned_throwsDuplicateResourceException() {
        User user = new User("alice", "pass", true);
        user.setId(1L);
        Role role = new Role("ADMIN");
        role.setId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
        when(userRoleRepository.existsByUserIdAndRoleId(1L, 2L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userRoleService.assignRole(1L, 2L));
        verify(userRoleRepository, never()).save(any(UserRole.class));
    }
}

