package club.bayview.smoothieweb.models;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a user on the site.
 */

@Document
@Getter
@Setter
public class User implements UserDetails {

    @Id // do not use @mongoid
    private String id;
    @Indexed(unique = true)
    private String handle;

    @Indexed(unique = true)
    private String email;
    private boolean enabled; // enabled by email verification

    private String password; // argon2id

    // profile
    private String description;
    private double score;

    private Set<Role> roles;

    private List<ObjectId> submissions;

    private List<ObjectId> solved;

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public User(String handle, String email, String password) {
        super();
        this.handle = handle;
        this.email = email;
        this.password = password; // encoded to argon2 in smoothieuserdetailsservice
        this.submissions = new ArrayList<>();
        this.solved = new ArrayList<>();
        this.enabled = false;
        roles = new HashSet<>(Arrays.asList(Role.ROLE_USER));
    }

    // ~~~~~

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(authority -> new SimpleGrantedAuthority(authority.getName())).collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return getHandle();
    }

    @Override
    public boolean isAccountNonExpired() {
        return false;
    }

    @Override
    public boolean isAccountNonLocked() {
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return false;
    }

    // ~~~~~

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((email == null) ? 0 : email.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        User user = (User) obj;
        return handle.equals(user.handle);
    }
}
