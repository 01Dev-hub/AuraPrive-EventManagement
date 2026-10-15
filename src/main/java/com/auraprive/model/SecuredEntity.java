package com.auraprive.model;

/**
 * Contract enforcing discrete security levels on all high-profile entities.
 */
public interface SecuredEntity {
    String getSecurityClearance();
    boolean verifyAccess();
}