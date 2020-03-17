package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.Role;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

@Service
public class SmoothieUserService implements ReactiveUserDetailsService {

    @Autowired
    UserRepository userRepository;

    public Mono<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Mono<User> findUserByHandle(String handle) {
        return userRepository.findByHandle(handle);
    }

    public Mono<User> findUserById(String id) {
        return userRepository.findById(id);
    }

    // sets

    public Flux<User> findUsersWithIds(List<String> ids) {
        return userRepository.findAllByIdIn(ids);
    }

    public Flux<User> findUsersWithIds(Flux<String> ids) {
        return userRepository.findAllByIdIn(ids);
    }

    public Flux<User> findUsersWithHandles(List<String> handles) {
        return userRepository.findAllByHandleIn(handles);
    }

    public Flux<String> resolveHandlesToIds(List<String> handles) {
        return findUsersWithHandles(handles).map(User::getId);
    }

    public Flux<String> resolveIdsToHandles(List<String> ids) {
        return findUsersWithIds(ids).map(User::getHandle);
    }

    public Mono<HashMap<String, User>> getUserIdToUserMap(Flux<String> ids) {
        HashMap<String, User> h = new HashMap<>();
        return findUsersWithIds(ids)
                .doOnNext(u -> h.put(u.getId(), u))
                .then(Mono.just(h));
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
                    // update user object for all sessions
//                    for (Session s : sessions.findByPrincipalName(user.getHandle()).values()) {
//                        SecurityContext securityContext = s.getAttribute("SPRING_SECURITY_CONTEXT");
//                        if (securityContext != null) {
//                            securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities()));
//                        }
//                    }
                });
    }

    private List<GrantedAuthority> getUserAuthority(Set<Role> userRoles) {
        Set<GrantedAuthority> roles = new HashSet<>();
        userRoles.forEach((role) -> roles.add(new SimpleGrantedAuthority(role.toString())));

        List<GrantedAuthority> grantedAuthorities = new ArrayList<>(roles);
        return grantedAuthorities;
    }

}
