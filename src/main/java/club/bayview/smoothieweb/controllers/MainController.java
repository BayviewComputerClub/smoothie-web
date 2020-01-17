package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.services.SmoothieUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@Controller
public class MainController {

    @Autowired
    private SmoothieUserService userService;

    @RequestMapping("/ranking")
    public Mono<String> getRankingRoute(Model model) {
        return userService.findUsers().collectList().flatMap(users -> {
            model.addAttribute("users", users);
            return Mono.just("ranking");
        });
    }

}
