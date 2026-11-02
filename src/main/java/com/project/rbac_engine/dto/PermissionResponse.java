package com.project.rbac_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response payload returned after creating a permission.
 * Encapsulates data without exposing the internal Permission entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {

    private Long id;
    private String name;
}
