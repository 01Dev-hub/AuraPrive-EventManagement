package com.auraprive.exception;

/**
 * Custom application exception handling security breaches and unauthorized inquiries.
 */
public class ExclusiveAccessException extends Exception {
    public ExclusiveAccessException(String message) {
        super("[AURA PRIVÉ SECURITY] " + message);
    }
}