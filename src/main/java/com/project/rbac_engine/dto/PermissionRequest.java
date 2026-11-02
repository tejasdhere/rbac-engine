package com.project.rbac_engine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for creating a new permission.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermissionRequest {

    @NotBlank(message = "Permission name must not be blank")
    @Size(max = 100, message = "Permission name must not exceed 100 characters")
    private String name;
}
