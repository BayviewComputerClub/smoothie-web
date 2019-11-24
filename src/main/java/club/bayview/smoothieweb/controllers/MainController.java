package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.services.SmoothieUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@Controller
public class MainController {

    @Autowired
    private SmoothieUserService userService;

    @GetMapping("/")
    public Mono<String> getRootRoute(Model model) {

        SecurityContext context = SecurityContextHolder.getContext();
        Authentication auth = context.getAuthentication();

        if (auth.isAuthenticated()) {
            User user = (User) userService.findByUsername(auth.getName()).block();
            if (user != null) {
                model.addAttribute("user", user);
            }
        }
        return Mono.just("index");

    }

    @RequestMapping("/ranking")
    public String getRankingRoute(Model model) {
        model.addAttribute("users", userService.findUsers().collectList().block());
        return "ranking";
    }

}
