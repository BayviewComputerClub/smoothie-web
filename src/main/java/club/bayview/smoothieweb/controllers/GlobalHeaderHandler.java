package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.Contest;
import club.bayview.smoothieweb.models.Role;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.services.SmoothieContestService;
import club.bayview.smoothieweb.services.SmoothieSettingsService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import reactor.core.publisher.Mono;

import java.security.Principal;

@ControllerAdvice
public class GlobalHeaderHandler {

    @Autowired
    SmoothieSettingsService settingsService;

    @Autowired
    SmoothieUserService userService;

    @Autowired
    SmoothieContestService contestService;

    @ModelAttribute("siteName")
    public String getSiteName() {
        return settingsService.getGeneralSettings().getSiteName();
    }

    @ModelAttribute("isAdmin")
    public Mono<Boolean> isAdmin(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof User))
            return Mono.just(false);

        User u = (User) auth.getPrincipal();
        return Mono.just(u.getRoles().contains(Role.ROLE_ADMIN));
    }

    @ModelAttribute("currentContest")
    public Mono<Contest> getCurrentContest(Principal p) {
        // get current contest, if the user is in one
        if (p instanceof User) {
            return userService.findUserById(((User) p).getId())
                    .flatMap(u -> {
                        if (u.getContestId() != null) {
                            return contestService.findContestById(u.getContestId());
                        }
                        return Mono.empty();
                    });
        }
        return Mono.empty();
    }

}
