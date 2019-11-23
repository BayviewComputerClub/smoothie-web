package club.bayview.smoothieweb.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Collection;

/**
 * Represents a role that can be applied to a user for permissions.
 */

@Document
public class Role {

    public static final Role DEFAULT_ROLE = new Role("DEFAULT_ROLE"),
            ADMIN_ROLE = new Role("ADMIN_ROLE"),
            EDITOR_ROLE = new Role("EDITOR_ROLE");

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    @DBRef
    private Collection<User> users;

    @DBRef
    private Collection<Privilege> privileges;

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public Role(String name) {
        super();
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<User> getUsers() {
        return users;
    }

    public void setUsers(Collection<User> users) {
        this.users = users;
    }

    public Collection<Privilege> getPrivileges() {
        return privileges;
    }

    public void setPrivileges(Collection<Privilege> privileges) {
        this.privileges = privileges;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final Role role = (Role) obj;
        return name.equals(role.name);
    }
}
