package club.bayview.smoothieweb.security;

import club.bayview.smoothieweb.services.SmoothieUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class SmoothieAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private SmoothieUserService userService;

    public Argon2PasswordEncoder passwordEncoder = new Argon2PasswordEncoder();

    @Override
    public Authentication authenticate(Authentication authentication) {
        String username = authentication.getName(), password = authentication.getCredentials().toString();

        UserDetails user = userService.findByUsername(username).block();

        System.out.println("-=-=-=-=-=-=- TEMP USER THING " + user + " " + (user == null ? null : new Argon2PasswordEncoder().matches(password, user.getPassword())));

        if (user == null) return null;
        if (!passwordEncoder.matches(password, user.getPassword())) return null;
        return new UsernamePasswordAuthenticationToken(user, password, user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
