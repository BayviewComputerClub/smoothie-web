package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.Contest;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.services.SmoothieContestService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import club.bayview.smoothieweb.util.ErrorCommon;
import club.bayview.smoothieweb.util.NoPermissionException;
import club.bayview.smoothieweb.util.NotFoundException;
import club.bayview.smoothieweb.util.PageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;

@Controller
public class ContestController {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    SmoothieContestService contestService;

    @Autowired
    SmoothieUserService userService;

    @GetMapping("/contests")
    public Mono<String> getContestsRoute(Model model,
                                         Authentication auth,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = PageUtil.DEFAULT_PAGE_SIZE) int pageSize) {
        return Mono.zip(
                contestService.countAllContests(),
                contestService.findAllContests(PageUtil.createPageable(page, pageSize, true, "timeCreated", model))
                        .filter(c -> c.hasPermissionToView(auth))
                        .collectList()
        ).flatMap(t -> {
            model.addAttribute(PageUtil.NUM_OF_ENTRIES, t.getT1());
            model.addAttribute("contests", t.getT2());
            return Mono.just("contests");
        });
    }

    @GetMapping("/contest/{name}")
    public Mono<String> getContestRoute(@PathVariable String name, Model model, Authentication auth) {
        return contestService.findContestByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(c -> {
                    if (!c.hasPermissionToView(auth))
                        return Mono.error(new NoPermissionException());

                    model.addAttribute("contest", c);
                    return Mono.just("contest");
                })
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "GET /contest/{name} route exception: "));
    }

    @GetMapping("/contest/{name}/leaderboard")
    public Mono<String> getContestLeaderboard(@PathVariable String name, Model model, Authentication auth) {
        return contestService.findContestByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMapMany(c -> {
                    if (!c.hasPermissionToView(auth))
                        return Flux.error(new NoPermissionException());

                    model.addAttribute("contest", c);

                    return userService.findUsersWithIds(Flux.fromStream(c.getParticipants().values().stream().filter(p -> p.getUserId() != null).map(Contest.ContestUser::getUserId)));
                })
                .collectList()
                .doOnNext(users -> {
                    HashMap<String, String> m = new HashMap<>();
                    users.forEach(u -> m.put(u.getId(), u.getHandle()));

                    model.addAttribute("users", m);
                })
                .then(Mono.just("contest-leaderboard"))
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "GET /contest/{name}/leaderboard route exception: "));
    }

    @PostMapping("/contest/{name}/join")
    public Mono<String> postContestJoin(@PathVariable String name, Authentication auth) {
        return contestService.findContestByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(c -> {
                    if (!c.hasPermissionToView(auth))
                        return Mono.error(new NoPermissionException());

                    if (auth == null || !(auth.getPrincipal() instanceof User))
                        return Mono.error(new NoPermissionException());

                    return Mono.zip(userService.findUserById(((User) auth.getPrincipal()).getId()), Mono.just(c));
                })
                .flatMap(t -> {
                    // set the user's contestId
                    t.getT1().setContestId(t.getT2().getId());

                    // add user as participant if not already
                    if (!t.getT2().getParticipants().containsKey(t.getT1().getId())) {
                        t.getT2().getParticipants().put(t.getT1().getId(), Contest.ContestUser.getDefault(t.getT2(), t.getT1()));
                    }

                    // update contest leaderboard
                    t.getT2().updateLeaderBoard();

                    // save
                    return Mono.zip(userService.saveUser(t.getT1()), contestService.saveContest(t.getT2()));
                })
                .then(Mono.just("redirect:/contest/" + name + "/problems"))
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "POST /contest/{name}/join"));
    }

    @PostMapping("/contest/{name}/leave")
    public Mono<String> postContestLeave(@PathVariable String name, Authentication auth) {
        // this just sets the contestId field to null, regardless of whether the user is in a contest
        if (auth != null && auth.getPrincipal() instanceof User) {
            return userService.findUserById(((User) auth.getPrincipal()).getId())
                    .switchIfEmpty(Mono.error(new NotFoundException()))
                    .flatMap(u -> {
                        u.setContestId(null);
                        return userService.saveUser(u);
                    })
                    .then(Mono.just("redirect:/"));

        } else {
            return Mono.just("redirect:/");
        }
    }
}
