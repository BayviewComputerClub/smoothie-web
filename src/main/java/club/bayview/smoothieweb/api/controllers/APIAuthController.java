package club.bayview.smoothieweb.api.controllers;

import club.bayview.smoothieweb.security.SmoothieAuthenticationProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
public class APIAuthController {

    @Autowired
    SmoothieAuthenticationProvider authenticationProvider;

    @Getter
    @Setter
    @AllArgsConstructor
    @ToString
    class AuthReq {
        String username;
        String password;
    }

    @PostMapping("/api/v1/login")
    public Mono<Void> postLogin(@Valid AuthReq authReq, ServerHttpResponse res, WebSession session) {
        var tok = new UsernamePasswordAuthenticationToken(authReq.getUsername(), authReq.getPassword());
        SecurityContext c = SecurityContextHolder.getContext();

        return authenticationProvider.authenticate(tok)
                .flatMap(t -> {
                    c.setAuthentication(t);
                    session.getAttributes().put(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, c);
                    res.setStatusCode(HttpStatus.OK);
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    res.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return Mono.empty();
                }).then();
    }

    @GetMapping("/api/v1/auth-status")
    public boolean getAuthStatus(Authentication auth) {
        return auth != null;
    }
    @GetMapping("/api/v1/auth-info")
    public Authentication getAuthInfo(Authentication auth) {
        return auth;
    }
}
