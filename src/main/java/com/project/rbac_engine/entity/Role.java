package com.project.rbac_engine.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a role (e.g. ADMIN, USER) stored in the "roles" table.
 * Roles are plain data rows — the name is never checked in code.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Role name must be unique; it is only a label, never used in authorization logic.
    @Column(unique = true, nullable = false)
    private String name;

    /** Convenience constructor used by DataSeeder (id is auto-generated). */
    public Role(String name) {
        this.name = name;
    }
}

