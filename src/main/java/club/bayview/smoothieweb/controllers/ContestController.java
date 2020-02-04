package club.bayview.smoothieweb.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

@Controller
public class ContestController {

    @GetMapping("/contest/{name}")
    public Mono<String> getContestRoute(@PathVariable String name, Model model) {
        return Mono.just("contest");
    }

    @GetMapping("/contest/{name}/leaderboard")
    public Mono<String> getContestLeaderboard(@PathVariable String name, Model model) {
        return Mono.just("contest");
    }

}
