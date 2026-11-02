package com.project.rbac_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response payload returned after creating a role.
 * Encapsulates data without exposing the internal Role entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {

    private Long id;
    private String name;
}
