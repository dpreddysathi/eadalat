package com.eadalat.auth.dto;

import com.eadalat.auth.entity.Role;
import com.eadalat.auth.entity.User;

/**
 * Safe user view — never exposes the password hash.
 */
public record UserResponse(
        Long id,
        String name,
        String email,
        Role role
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
