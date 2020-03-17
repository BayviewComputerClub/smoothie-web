package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.services.SmoothieSettingsService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@Controller
public class MainController {

    @Autowired
    SmoothieUserService userService;

    @Autowired
    SmoothieSettingsService settingsService;

    @RequestMapping("/ranking")
    public Mono<String> getRankingRoute(Model model) {
        return userService.findUsers().collectList().flatMap(users -> {
            model.addAttribute("users", users);
            return Mono.just("ranking");
        });
    }

    @RequestMapping("/")
    public Mono<String> getHomeRoute(Model model) {
        model.addAttribute("information", settingsService.getGeneralSettings());
        return Mono.just("index");
    }

    @RequestMapping("/no")
    public Mono<String> getNoRoute() {
        return Mono.just("error/no");
    }

}
