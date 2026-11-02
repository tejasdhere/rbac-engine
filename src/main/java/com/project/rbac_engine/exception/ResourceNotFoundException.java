package com.project.rbac_engine.exception;

/**
 * Thrown when an entity (User, Role, Permission) is not found in the database.
 * Will be mapped to HTTP 404 in GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
