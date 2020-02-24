package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.services.SmoothieContestService;
import club.bayview.smoothieweb.util.ErrorCommon;
import club.bayview.smoothieweb.util.NoPermissionException;
import club.bayview.smoothieweb.util.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

@Controller
public class ContestController {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    SmoothieContestService contestService;

    @GetMapping("/contests")
    public Mono<String> getContestsRoute(Model model) {
        return contestService.findAllContests().collectList().flatMap(cs -> {
            model.addAttribute("contests", cs);
            return Mono.just("contests");
        });
    }

    @GetMapping("/contest/{name}")
    public Mono<String> getContestRoute(@PathVariable String name, Model model, Authentication auth) {
        return contestService.findContestByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(c -> {
                    if (!c.hasPermissionToView(auth)) {
                        return Mono.error(new NoPermissionException());
                    }
                    model.addAttribute("contest", c);
                    return Mono.just("contest");
                })
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "GET /contest/{name} route exception: "));
    }

    @GetMapping("/contest/{name}/leaderboard")
    public Mono<String> getContestLeaderboard(@PathVariable String name, Model model, Authentication auth) {

        return contestService.findContestByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(c -> {
                    if (!c.hasPermissionToView(auth))
                        return Mono.error(new NoPermissionException());

                    model.addAttribute("contest", c);
                    return Mono.just("contest-leaderboard");
                })
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "GET /contest/{name}/leaderboard route exception: "));
    }
}
