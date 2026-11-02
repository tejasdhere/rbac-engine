package com.project.rbac_engine.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a fine-grained permission (e.g. ROLE_CREATE) stored in the "permissions" table.
 * These names are the strings referenced in @PreAuthorize("hasPermission(...)").
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Permission name must be unique; it is the key used by CustomPermissionEvaluator.
    @Column(unique = true, nullable = false)
    private String name;

    /** Convenience constructor used by DataSeeder (id is auto-generated). */
    public Permission(String name) {
        this.name = name;
    }
}

