package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.services.SmoothieUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.thymeleaf.spring5.context.webflux.IReactiveDataDriverContextVariable;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Mono;

@Controller
public class MainController {

    @Autowired
    private SmoothieUserService userDetailsService;

    @GetMapping("/")
    public Mono<String> getRootRoute(Model model) {

        SecurityContext context = SecurityContextHolder.getContext();
        Authentication auth = context.getAuthentication();

        if (auth.isAuthenticated()) {
            model.addAttribute("name", auth.getName());

            User user = (User) userDetailsService.findByUsername(auth.getName()).block();
            if (user != null) {
                System.out.println("hmm");
                model.addAttribute("user", user);
            }
        }
        return Mono.just("index");

    }

}
