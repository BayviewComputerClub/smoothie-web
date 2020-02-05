package club.bayview.smoothieweb.security;

import club.bayview.smoothieweb.services.SmoothieUserService;
import club.bayview.smoothieweb.util.NotFoundException;
import club.bayview.smoothieweb.util.SmUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.HashMap;

@Component
public class UpdateAuthFilter implements WebFilter {
    public static final String UPDATED_ATTRIBUTE = "PRINCIPAL_LAST_UPDATED";

    public HashMap<String, Long> sessionsToUpdate = new HashMap<>();

    @Autowired
    SmoothieUserService userService;

    // update authentication when user is updated
    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain filterChain) {
        final SecurityContextImpl[] securityContext = new SecurityContextImpl[1];


        return Mono.empty()
                .flatMap(b -> {
                    if (serverWebExchange.getAttribute(UPDATED_ATTRIBUTE) == null) {
                        serverWebExchange.getAttributes().put(UPDATED_ATTRIBUTE, SmUtil.getCurrentUnix());
                        return Mono.empty();
                    }

                    securityContext[0] = serverWebExchange.getAttribute("SPRING_SECURITY_CONTEXT");

                    if (securityContext[0] != null) {
                        String userName = securityContext[0].getAuthentication().getName();

                        if (sessionsToUpdate.containsKey(userName)) {
                            if (serverWebExchange.getAttribute(UPDATED_ATTRIBUTE) instanceof Long && sessionsToUpdate.get(userName) < (Long) serverWebExchange.getAttribute(UPDATED_ATTRIBUTE)) {
                                return userService.findByUsername(userName);
                            }
                        }
                    }

                    return Mono.empty();
                })
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .doOnNext(user -> {
                    Authentication newAuth = new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
                    securityContext[0].setAuthentication(newAuth);
                    serverWebExchange.getAttributes().put("SPRING_SECURITY_CONTEXT", securityContext[0]);
                    serverWebExchange.getAttributes().put(UPDATED_ATTRIBUTE, SmUtil.getCurrentUnix());
                })
                .doOnError(b -> filterChain.filter(serverWebExchange))
                .then()
                .doOnNext(b -> filterChain.filter(serverWebExchange))
                .then();
    }
}
