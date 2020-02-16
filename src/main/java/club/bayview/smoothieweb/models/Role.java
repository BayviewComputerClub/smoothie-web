package club.bayview.smoothieweb.models;

import java.io.Serializable;

/**
 * Represents a role that can be applied to a user for permissions.
 */

public enum Role implements Serializable {
    ROLE_ADMIN("ROLE_ADMIN"),
    ROLE_USER("ROLE_USER"),
    ROLE_EDITOR("ROLE_EDITOR");

    private final String name;

    Role(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
