package com.aalap.aalapbackend.entity;

/**
 * User roles — foundation for RBAC.
 * Currently all users are assigned ROLE_USER at authentication time.
 * When admin functionality is needed:
 *   1. Add a `role` VARCHAR column to the `users` table (DEFAULT 'USER').
 *   2. Add `@Enumerated(EnumType.STRING) private Role role = Role.USER;` to User entity.
 *   3. Update User.getAuthorities() to read from the persisted field.
 */
public enum Role {
    USER,
    ADMIN
}

