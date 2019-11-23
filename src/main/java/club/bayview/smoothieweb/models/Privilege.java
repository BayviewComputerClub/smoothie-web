package club.bayview.smoothieweb.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a privilege attribute (permission) that is given to a role.
 */

@Document
public class Privilege {

    public static final Privilege ADD_PROBLEM_PRIVILEGE = new Privilege("ADD_PROBLEM_PRIVILEGE");

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public Privilege(String name) {
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

        Privilege other = (Privilege) obj;
        if (name == null && other.name != null) return false;
        return name == null || name.equals(other.name);
    }


}
