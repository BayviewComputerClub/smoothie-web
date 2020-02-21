package club.bayview.smoothieweb.controllers.admin;

import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.models.Contest;
import club.bayview.smoothieweb.services.SmoothieContestService;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import club.bayview.smoothieweb.util.ErrorCommon;
import club.bayview.smoothieweb.util.NotFoundException;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Controller
public class AdminContestController {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    SmoothieProblemService problemService;

    @Autowired
    SmoothieContestService contestService;

    @Autowired
    SmoothieUserService userService;

    /* ~~~~~ Routes ~~~~~ */

    @GetMapping("/contest/{name}/admin")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getContestDashboard(@PathVariable String name, Model model) {
        return Mono.just("contest");
    }

    @GetMapping("/admin/new-contest")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getNewContestRoute(Model model) {

    }

    @PostMapping("/admin/new-contest")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> postNewContestRoute(@Valid Contest.ContestProblem contest, BindingResult result, Model model) {

    }

    @GetMapping("/contest/{name}/edit")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getContestEditRoute(@PathVariable String name, Model model) {

    }

    @PostMapping("/contest/{name}/edit")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> postContestEditRoute(@PathVariable String name, @Valid Contest.ContestProblem contest, BindingResult result, Model model) {

    }

    @GetMapping("/contest/{name}/delete")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getContestDeleteRoute(@PathVariable String name, Model model) {
        if (name == null) return Mono.just("404");

        return contestService.findContestByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(c -> {
                    model.addAttribute("contest", c);
                    return Mono.just("admin/delete-contest");
                })
                .onErrorResume(e -> ErrorCommon.handle404(e, logger, "POST /contest/{name}/delete route exception: "));
    }

    @PostMapping("/contest/{name}/delete")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> postContestDeleteRoute(@PathVariable String name, Model model) {
        if (name == null) return Mono.just("404");

        return contestService.findContestByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(c -> contestService.deleteContestById(c.getId()))
                .flatMap(b -> Mono.just("redirect:/contests"))
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "POST /contest/{name}/delete route exception: "));
    }
}
