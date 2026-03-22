package com.realestate.app.auth;

import java.io.Serial;
import java.io.Serializable;

public class SessionUser implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String displayName;
    private final String email;
    private final String role;

    public SessionUser(String id, String displayName, String email, String role) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}