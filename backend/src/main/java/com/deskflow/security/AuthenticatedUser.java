package com.deskflow.security;

import com.deskflow.user.Role;

/** The logged-in user, read from the JWT so that most requests need no database lookup. */
public record AuthenticatedUser(Long id, String email, Role role) {

    public boolean isStaff() {
        return role.isStaff();
    }
}
