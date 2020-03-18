package club.bayview.smoothieweb.controllers.admin;

import club.bayview.smoothieweb.models.ContestForm;
import club.bayview.smoothieweb.services.SmoothieContestService;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import club.bayview.smoothieweb.util.ErrorCommon;
import club.bayview.smoothieweb.util.NoPermissionException;
import club.bayview.smoothieweb.util.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

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
    public Mono<String> getContestDashboard(@PathVariable String name, Model model, Authentication auth) {
        return contestService.findContestByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(c -> {
                    if (!c.hasPermissionToManage(auth))
                        return Mono.error(new NoPermissionException());

                    model.addAttribute("contest", c);
                    return Mono.just("admin/manage-contest");
                })
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "GET /contest/{name}/admin route exception: "));
    }

    @GetMapping("/admin/contests")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getAdminContestsRoute(Model model) {
        return contestService.findAllContests(Pageable.unpaged())
                .collectList()
                .doOnNext(cs -> model.addAttribute("contests", cs))
                .flatMap(cs -> Mono.just("admin/contests"));
    }

    @GetMapping("/admin/new-contest")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getNewContestRoute(Model model) {
        model.addAttribute("newContest", true);
        model.addAttribute("form", ContestForm.defaultContest);
        return Mono.just("admin/edit-contest");
    }

    @PostMapping("/admin/new-contest")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> postNewContestRoute(@Valid ContestForm contestForm, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("newContest", true);
            return Mono.just("admin/edit-contest");
        }

        // TODO check if contest exists
        return contestForm.toContest(null)
                .flatMap(c -> contestService.saveContest(c))
                .flatMap(c -> Mono.just("redirect:/contest/" + c.getName() + "/admin"))
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "POST /admin/new-contest route exception: "));
    }

    @GetMapping("/contest/{name}/edit")
    public Mono<String> getContestEditRoute(@PathVariable String name, Model model, Authentication auth) {

        return contestService.findContestByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(c -> {
                    if (!c.hasPermissionToEdit(auth))
                        return Mono.error(new NoPermissionException());

                    model.addAttribute("form", ContestForm.fromContest(c));
                    return Mono.just("admin/edit-contest");
                })
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "GET /contest/{name}/edit route exception: "));
    }

    @PostMapping("/contest/{name}/edit")
    public Mono<String> postContestEditRoute(@PathVariable String name, @Valid ContestForm contestForm, BindingResult result, Authentication auth) {
        if (result.hasErrors()) {
            return Mono.just("admin/edit-contest");
        }

        // TODO check if contest name exists
        return contestService.findContestByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(c -> {
                    if (!c.hasPermissionToEdit(auth))
                        return Mono.error(new NoPermissionException());

                    return contestForm.toContest(c);
                })
                .flatMap(c -> contestService.saveContest(c))
                .flatMap(c -> Mono.just("redirect:/contest/" + c.getName() + "/admin"))
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "POST /contest/{name}/edit route exception: "));
    }

    @GetMapping("/contest/{name}/delete")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getContestDeleteRoute(@PathVariable String name, Model model) {
        return contestService.findContestByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .doOnNext(c -> model.addAttribute("contest", c))
                .flatMap(c -> Mono.just("admin/delete-contest"))
                .onErrorResume(e -> ErrorCommon.handle404(e, logger, "POST /contest/{name}/delete route exception: "));
    }

    @PostMapping("/contest/{name}/delete")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> postContestDeleteRoute(@PathVariable String name, Model model) {

        return contestService.findContestByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(c -> contestService.deleteContestById(c.getId()))
                .then(Mono.just("redirect:/admin/contests"))
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "POST /contest/{name}/delete route exception: "));
    }
}
