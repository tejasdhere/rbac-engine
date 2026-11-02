package com.project.rbac_engine.security;

import com.project.rbac_engine.entity.User;
import com.project.rbac_engine.repository.PermissionRepository;
import com.project.rbac_engine.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito unit test — no Spring context.
 * Verifies every guard condition and the happy path in CustomPermissionEvaluator.
 */
@ExtendWith(MockitoExtension.class)
class CustomPermissionEvaluatorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private CustomPermissionEvaluator evaluator;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = new User("alice", "hashed", true);
        alice.setId(1L);
    }

    private Authentication createMockAuth(String username) {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn(username);
        return auth;
    }

    // ── Happy path ──────────────────────────────────────────────────────────────

    @Test
    void returnsTrue_whenUserHasPermission() {
        Authentication auth = createMockAuth("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(permissionRepository.findPermissionNamesByUserId(1L))
                .thenReturn(List.of("ROLE_CREATE", "PERMISSION_CREATE"));

        assertTrue(evaluator.hasPermission(auth, null, "ROLE_CREATE"));
    }

    // ── Negative paths ───────────────────────────────────────────────────────────

    @Test
    void returnsFalse_whenUserLacksPermission() {
        Authentication auth = createMockAuth("alice");
        // User exists but does not hold the requested permission.
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(permissionRepository.findPermissionNamesByUserId(1L))
                .thenReturn(List.of("SECURE_DATA_READ"));

        assertFalse(evaluator.hasPermission(auth, null, "ROLE_CREATE"));
    }

    @Test
    void returnsFalse_whenUserNotFound() {
        Authentication auth = createMockAuth("alice");
        // Principal is authenticated but no matching row in the DB — deny safely.
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        assertFalse(evaluator.hasPermission(auth, null, "ROLE_CREATE"));
    }

    @Test
    void returnsFalse_whenAuthenticationIsNull() {
        // No principal at all — should not reach the DB.
        assertFalse(evaluator.hasPermission(null, null, "ROLE_CREATE"));
    }

    @Test
    void returnsFalse_whenPermissionIsNotAString() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        // Non-string permission should fail immediately without reading name or DB.
        assertFalse(evaluator.hasPermission(auth, null, 42));
    }

    @Test
    void returnsFalse_whenAuthenticationIsNotAuthenticated() {
        // Token exists but isAuthenticated() == false (e.g. anonymous token).
        Authentication unauthenticated = mock(Authentication.class);
        when(unauthenticated.isAuthenticated()).thenReturn(false);

        assertFalse(evaluator.hasPermission(unauthenticated, null, "ROLE_CREATE"));
    }

    // ── 4-argument overload ──────────────────────────────────────────────────────

    @Test
    void fourArgVariant_delegatesToMainMethod() {
        Authentication auth = createMockAuth("alice");
        // The 4-arg overload must produce the same result as the 3-arg method.
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(permissionRepository.findPermissionNamesByUserId(1L))
                .thenReturn(List.of("USER_ROLE_ASSIGN"));

        // targetId = 99L, targetType = "Role" — neither is used in our implementation.
        assertTrue(evaluator.hasPermission(auth, 99L, "Role", "USER_ROLE_ASSIGN"));
    }
}
