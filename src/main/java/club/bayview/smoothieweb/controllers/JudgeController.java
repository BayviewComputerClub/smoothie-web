package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.JudgeLanguage;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.services.SmoothieSubmissionService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;

@Controller
public class JudgeController {

    @Autowired
    private SmoothieProblemService problemService;

    @Autowired
    private SmoothieSubmissionService submissionService;

    @RequestMapping("/contests")
    public String getContestsRoute(Model model) {
        return "contests";
    }

    @RequestMapping("/problems")
    public Mono<String> getProblemsRoute(Model model) {
        return problemService.findProblems().collectList().flatMap(problems -> {
            model.addAttribute("problems", problems);
            return Mono.just("problems");
        });
    }

    @GetMapping("/problem/{name}")
    public Mono<String> getProblemRoute(@PathVariable String name, Model model) {
        return problemService.findProblemByName(name).flatMap(p -> {
           if (p == null) return Mono.just("404");

           model.addAttribute("problem", p);
           return Mono.just("problem");
        });
    }

    @Getter
    @Setter
    private static class SubmitRequest {
        @NotNull
        private String code;

        @NotNull
        private String language;
    }

    @GetMapping("/problem/{name}/submit")
    public Mono<String> getProblemSubmitRoute(@PathVariable String name, Model model) {

        return problemService.findProblemByName(name).flatMap(p -> {
            if (p == null) return Mono.just("404");

            model.addAttribute("problemName", p.getPrettyName());
            model.addAttribute("submitRequest", new SubmitRequest());
            model.addAttribute("languages", JudgeLanguage.values);
            return Mono.just("submit");
        });
    }

}
