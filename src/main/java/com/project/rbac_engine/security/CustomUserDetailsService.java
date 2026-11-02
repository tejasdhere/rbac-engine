package com.project.rbac_engine.security;

import com.project.rbac_engine.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads a user from the database by username during HTTP Basic authentication.
 *
 * WHY empty authorities: Spring Security needs a UserDetails object to authenticate,
 * but we deliberately leave the authorities list empty. Authorization (what the user
 * can DO) is handled exclusively by CustomPermissionEvaluator via @PreAuthorize.
 * Putting roles in authorities would be a second, redundant authorization path.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Constructor injection: no @Autowired needed in Spring 4+; keeps class testable.
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.project.rbac_engine.entity.User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        // UsernameNotFoundException triggers a 401, not a 404, by Spring Security design.
                        new UsernameNotFoundException("No user found with username: " + username));

        // Build Spring Security's built-in User record. Password is already BCrypt-hashed in the DB.
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .authorities(List.of())   // intentionally empty — see class Javadoc
                .build();
    }
}
