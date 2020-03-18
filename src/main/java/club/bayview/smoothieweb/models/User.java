package club.bayview.smoothieweb.models;

import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.security.SmoothieAuthenticationProvider;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a user on the site.
 */

@Document(collation = "{ 'locale' : 'en_US', 'strength': 2 }") // indexes case insensitive
@Getter
@Setter
@ToString
public class User implements UserDetails, Serializable {

    @Id // do not use @mongoid
    private String id;
    @Indexed(unique = true)
    private String handle;

    @Indexed(unique = true)
    private String email;
    private boolean enabled; // enabled by email verification

    @Indexed
    private List<String> userGroupIds;

    private String password; // argon2id

    // profile
    private String description;
    private double points;

    private Set<Role> roles;

    private List<String> solved; // solved problems (problemId)
    private HashMap<String, Double> problemsAttempted; // solved problems: <problemId, points gotten>

    private String contestId; // contest the user is currently in

    public User(String handle, String email, String password) {
        super();
        this.handle = handle;
        this.email = email;
        this.password = password;
        setPassword(password); // encoded to argon2
        this.solved = new ArrayList<>();
        this.problemsAttempted = new HashMap<>();
        this.enabled = false;
        this.description = "";
        roles = new HashSet<>(Arrays.asList(Role.ROLE_USER));
    }

    public boolean isPassword(String password) {
        return SmoothieWebApplication.context.getBean(SmoothieAuthenticationProvider.class).passwordEncoder.matches(password, getPassword());
    }

    /**
     * MUST be called when creating a user or update its password
     *
     * @param password plain-text password
     * @return Password encoded with Argon2PasswordEncoder
     */
    public static String encodePassword(String password) {
        return new Argon2PasswordEncoder().encode(password);
    }

    public boolean isAdmin() {
        return getRoles().contains(Role.ROLE_ADMIN);
    }

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
