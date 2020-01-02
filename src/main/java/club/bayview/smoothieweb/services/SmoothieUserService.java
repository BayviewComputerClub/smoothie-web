package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.Role;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.models.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SmoothieUserService implements ReactiveUserDetailsService {

    @Autowired
    private UserRepository userRepository;

    public Mono<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Mono<User> findUserByHandle(String handle) {
        return userRepository.findByHandle(handle);
    }

    public Mono<User> findUserById(String id) {
        return userRepository.findById(id);
    }

    public Flux<User> findUsersWithIds(List<String> ids) {
        return userRepository.findAllByIdIn(ids);
    }

    @Override
    public Mono<UserDetails> findByUsername(String handle) {
        return userRepository.findByHandle(handle).cast(UserDetails.class);
    }

    public Flux<User> findUsers() {
        return userRepository.findAll();
    }

    public Mono<User> saveUser(User user) {
        return userRepository.save(user);
    }

    private List<GrantedAuthority> getUserAuthority(Set<Role> userRoles) {
        Set<GrantedAuthority> roles = new HashSet<>();
        userRoles.forEach((role) -> roles.add(new SimpleGrantedAuthority(role.toString())));

        List<GrantedAuthority> grantedAuthorities = new ArrayList<>(roles);
        return grantedAuthorities;
    }

}
