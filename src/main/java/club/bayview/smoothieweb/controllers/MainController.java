package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.services.SmoothieUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import reactor.core.publisher.Mono;

@Controller
public class MainController {

    @Autowired
    private SmoothieUserService userService;

    @GetMapping("/")
    public Mono<String> getRootRoute(Model model) {
        return Mono.just("index");
    }

    @RequestMapping("/ranking")
    public Mono<String> getRankingRoute(Model model) {
        return userService.findUsers().collectList().flatMap(users -> {
            model.addAttribute("users", users);
            return Mono.just("ranking");
        });
    }

}
