package club.bayview.smoothieweb.security;

import club.bayview.smoothieweb.services.SmoothieUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SmoothieAuthenticationProvider implements ReactiveAuthenticationManager {

//    @Autowired
//    FindByIndexNameSessionRepository<? extends Session> sessions;

    @Autowired
    private SmoothieUserService userService;

    public PasswordEncoder passwordEncoder = new Argon2PasswordEncoder();

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String username = authentication.getName(), password = authentication.getCredentials().toString();

        return userService.findUserByHandle(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException(String.format("Username %s not found", username))))
                .flatMap(u -> {
                    if (!passwordEncoder.matches(password, u.getPassword()))
                        return Mono.error(new BadCredentialsException("Incorrect password"));

                    return Mono.just(new UsernamePasswordAuthenticationToken(u, passwordEncoder.encode(password), u.getAuthorities()));
                });
//        // update session
//        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest()
//                .getSession(true).setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, user.getHandle());
    }

}
