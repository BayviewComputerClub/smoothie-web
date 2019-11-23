package club.bayview.smoothieweb.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a user on the site.
 */

@Document
public class User implements UserDetails {

    @Id
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

    @DBRef
    private Set<Role> roles;

    @DBRef
    private Collection<Submission> submissions;

    @DBRef
    private Collection<Problem> solved;

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public User(String handle, String email, String password) {
        super();
        this.handle = handle;
        this.email = email;
        this.password = password; // encoded to argon2 in smoothieuserdetailsservice
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

    public String getId() {
        return id;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public Collection<Submission> getSubmissions() {
        return submissions;
    }

    public void setSubmissions(Collection<Submission> submissions) {
        this.submissions = submissions;
    }

    public Collection<Problem> getSolved() {
        return solved;
    }

    public void setSolved(Collection<Problem> solved) {
        this.solved = solved;
    }

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
