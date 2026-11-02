package com.project.rbac_engine.exception;

import java.time.Instant;

/**
 * Standard uniform error payload for all REST API error responses.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public ErrorResponse(int status, String error, String message, String path) {
        this(Instant.now(), status, error, message, path);
    }
}

