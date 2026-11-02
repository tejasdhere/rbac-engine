package com.project.rbac_engine.exception;

/**
 * Thrown when attempting to create a duplicate resource (e.g., duplicate role/permission name
 * or an already-existing assignment mapping).
 * Will be mapped to HTTP 409 in GlobalExceptionHandler.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
