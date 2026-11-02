package com.project.rbac_engine.config;

import com.project.rbac_engine.entity.Permission;
import com.project.rbac_engine.entity.Role;
import com.project.rbac_engine.entity.User;
import com.project.rbac_engine.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Test
    void run_firstTime_createsAllEntities() {
        when(permissionRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(permissionRepository.save(any(Permission.class))).thenAnswer(i -> {
            Permission p = i.getArgument(0);
            p.setId(1L);
            return p;
        });

        when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(i -> {
            Role r = i.getArgument(0);
            r.setId(1L);
            return r;
        });

        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(1L);
            return u;
        });

        when(rolePermissionRepository.existsByRoleIdAndPermissionId(anyLong(), anyLong())).thenReturn(false);
        when(userRoleRepository.existsByUserIdAndRoleId(anyLong(), anyLong())).thenReturn(false);

        dataSeeder.run();

        verify(permissionRepository, times(5)).save(any(Permission.class));
        verify(roleRepository, times(2)).save(any(Role.class));
        verify(userRepository, times(2)).save(any(User.class));
        verify(rolePermissionRepository, times(5)).save(any());
        verify(userRoleRepository, times(2)).save(any());
    }

    @Test
    void run_secondTime_isIdempotent_createsNoDuplicates() {
        Permission existingPerm = new Permission("PERM");
        existingPerm.setId(1L);
        Role existingRole = new Role("ROLE");
        existingRole.setId(1L);
        User existingUser = new User("user", "hash", true);
        existingUser.setId(1L);

        when(permissionRepository.findByName(anyString())).thenReturn(Optional.of(existingPerm));
        when(roleRepository.findByName(anyString())).thenReturn(Optional.of(existingRole));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(existingUser));

        when(rolePermissionRepository.existsByRoleIdAndPermissionId(anyLong(), anyLong())).thenReturn(true);
        when(userRoleRepository.existsByUserIdAndRoleId(anyLong(), anyLong())).thenReturn(true);

        dataSeeder.run();

        verify(permissionRepository, never()).save(any(Permission.class));
        verify(roleRepository, never()).save(any(Role.class));
        verify(userRepository, never()).save(any(User.class));
        verify(rolePermissionRepository, never()).save(any());
        verify(userRoleRepository, never()).save(any());
    }
}

