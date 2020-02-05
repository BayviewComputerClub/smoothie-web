package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.Role;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.repositories.UserRepository;
import club.bayview.smoothieweb.security.UpdateAuthFilter;
import club.bayview.smoothieweb.util.SmUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SmoothieUserService implements ReactiveUserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UpdateAuthFilter authFilter;

    @Autowired
    private FindByIndexNameSessionRepository<? extends Session> sessions;

    public Mono<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email).cache(Duration.ofMinutes(2));
    }

    public Mono<User> findUserByHandle(String handle) {
        return userRepository.findByHandle(handle).cache(Duration.ofMinutes(2));
    }

    public Mono<User> findUserById(String id) {
        return userRepository.findById(id).cache(Duration.ofMinutes(2));
    }

    public Flux<User> findUsersWithIds(List<String> ids) {
        return userRepository.findAllByIdIn(ids).cache(Duration.ofMinutes(2));
    }

    @Override
    public Mono<UserDetails> findByUsername(String handle) {
        return userRepository.findByHandle(handle).cast(UserDetails.class);
    }

    public Flux<User> findUsers() {
        return userRepository.findAll();
    }

    public Mono<User> saveUser(User user) {
        return userRepository.save(user)
                .doOnNext(u -> {
                    // update user authority
                    for (Session s : sessions.findByPrincipalName(user.getHandle()).values()) {
                        SecurityContext securityContext = s.getAttribute("SPRING_SECURITY_CONTEXT");
                        if (securityContext != null) {
                            securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities()));
                        }
                    }
                });
    }

    private List<GrantedAuthority> getUserAuthority(Set<Role> userRoles) {
        Set<GrantedAuthority> roles = new HashSet<>();
        userRoles.forEach((role) -> roles.add(new SimpleGrantedAuthority(role.toString())));

        List<GrantedAuthority> grantedAuthorities = new ArrayList<>(roles);
        return grantedAuthorities;
    }

}
