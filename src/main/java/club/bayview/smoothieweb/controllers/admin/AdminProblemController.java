package club.bayview.smoothieweb.controllers.admin;

import club.bayview.smoothieweb.models.JudgeLanguage;
import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.models.ProblemForm;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.util.ErrorCommon;
import club.bayview.smoothieweb.util.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
public class AdminProblemController {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    SmoothieProblemService problemService;

    // -=-=-=-=-=-=-=-=-=-=- Routes -=-=-=-=-=-=-=-=-=-=-

    @GetMapping("/admin/new-problem")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getNewProblemRoute(Model model) {
        model.addAttribute("newProblem", true);
        model.addAttribute("problem", ProblemForm.defaultProblem);
        model.addAttribute("languages", JudgeLanguage.values);
        return Mono.just("admin/edit-problem");
    }

    @GetMapping("/problem/{name}/edit")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getEditProblemRoute(@PathVariable String name, Model model) {
        if (name == null) return Mono.just("404");

        return problemService.findProblemByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(p -> {
                    model.addAttribute("newProblem", false);
                    model.addAttribute("problem", ProblemForm.fromProblem(p));
                    model.addAttribute("languages", JudgeLanguage.values);
                    return Mono.just("admin/edit-problem");
                }).onErrorResume(e -> ErrorCommon.handle404(e, logger, "GET /problem/{name}/edit route exception: "));
    }

    @GetMapping("/problem/{name}/manage")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getManageProblemRoute(@PathVariable String name, Model model) {
        if (name == null) return Mono.just("404");

        return problemService.findProblemByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(p -> {
                    model.addAttribute("problem", p);
                    return Mono.just("admin/manage-problem");
                }).onErrorResume(e -> ErrorCommon.handle404(e, logger, "GET /problem/{name}/manage route exception: "));
    }

    @GetMapping("/problem/{name}/delete")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getDeleteProblemRoute(@PathVariable String name, Model model) {
        if (name == null) return Mono.just("404");

        return problemService.findProblemByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(p -> {
                    model.addAttribute("problem", p);
                    return Mono.just("admin/delete-problem");
                })
                .onErrorResume(e -> ErrorCommon.handle404(e, logger, "GET /problem/{name}/delete route exception: "));
    }

    @PostMapping("/problem/{name}/delete")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> postDeleteProblemRoute(@PathVariable String name, Model model) {

        return problemService.findProblemByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(p -> problemService.deleteProblemById(p.getId()))
                .then(Mono.just("redirect:/problems")) // void mono MUST use then
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "POST /problem/{name}/delete route exception: "));
    }

    @GetMapping("/admin/problems")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<String> getAdminProblemsRoute(Model model) {
        return problemService.findProblems(Pageable.unpaged())
                .collectList()
                .doOnNext(problems -> model.addAttribute("problems", problems))
                .then(Mono.just("admin/problems"));
    }

    @PostMapping("/admin/new-problem")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> postNewProblemRoute(@Valid ProblemForm form, BindingResult result, Model model) {

        if (result.hasErrors()) { // TODO
            model.addAttribute("newProblem", true);
            model.addAttribute("problem", form);
            model.addAttribute("languages", JudgeLanguage.values);
            return Mono.just("admin/edit-problem");
        } else {
            Problem p = form.toProblem(null);
            p.setTimeCreated(System.currentTimeMillis());

            // TODO check if problem already exists

            return problemService.saveProblem(p)
                    .then(Mono.just("redirect:/problem/" + p.getName() + "/manage"));
        }
    }

    @PostMapping("/problem/{name}/edit")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> postEditProblemRoute(@PathVariable String name, @Valid ProblemForm form, BindingResult result, Model model) {
        if (result.hasErrors()) { // TODO form errors
            model.addAttribute("newProblem", false);
            model.addAttribute("problem", form);
            model.addAttribute("languages", JudgeLanguage.values);
            return Mono.just("admin/edit-problem");
        } else {
            // save problem

            // TODO check if problem handle already is used
            return problemService.findProblemByName(name)
                    .switchIfEmpty(Mono.error(new NotFoundException()))
                    .flatMap(originalProblem -> problemService.saveProblem(form.toProblem(originalProblem)))
                    .then(Mono.just("redirect:/problem/" + name))
                    .onErrorResume(e -> ErrorCommon.handle404(e, logger, "POST /problem/{name}/edit route exception: "));
        }
    }

}
