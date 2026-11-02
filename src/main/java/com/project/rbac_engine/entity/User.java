package com.project.rbac_engine.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an application user stored in the "users" table.
 * Password is stored as a BCrypt hash — never plaintext.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Username must be unique so we can look it up during authentication.
    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    // Allows disabling a user without deleting their record.
    @Column(nullable = false)
    private boolean enabled = true;

    /** Convenience constructor used by DataSeeder (id is auto-generated). */
    public User(String username, String password, boolean enabled) {
        this.username = username;
        this.password = password;
        this.enabled  = enabled;
    }
}

