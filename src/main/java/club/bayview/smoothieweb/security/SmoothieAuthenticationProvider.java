package club.bayview.smoothieweb.security;

import club.bayview.smoothieweb.services.SmoothieUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class SmoothieAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private SmoothieUserService userService;

    public PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Authentication authenticate(Authentication authentication) {
        String username = authentication.getName(), password = authentication.getCredentials().toString();

        UserDetails user = userService.findByUsername(username).block();

        System.out.println("-=-=-=-=-=-=- USER LOGIN ATTEMPT " + user + " " + (user == null ? null : passwordEncoder.matches(password, user.getPassword()))); // TODO

        if (user == null) return null;
        if (!passwordEncoder.matches(password, user.getPassword())) return null;
        return new UsernamePasswordAuthenticationToken(user, password, user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
