package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.SmoothieRunner;
import club.bayview.smoothieweb.models.JudgeLanguage;
import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.services.SmoothieRunnerService;
import club.bayview.smoothieweb.services.SmoothieSubmissionService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Controller
public class JudgeController {

    @Autowired
    private SmoothieProblemService problemService;

    @Autowired
    private SmoothieSubmissionService submissionService;

    @Autowired
    private SmoothieRunnerService runnerService;

    @Autowired
    private SmoothieUserService userService;

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
    @PreAuthorize("hasRole('ROLE_USER')")
    public Mono<String> getProblemSubmitRoute(@PathVariable String name, Model model) {
        return problemService.findProblemByName(name).flatMap(p -> {
            if (p == null) return Mono.just("404");

            model.addAttribute("problemName", p.getPrettyName());
            model.addAttribute("submitRequest", new SubmitRequest());
            model.addAttribute("languages", JudgeLanguage.getLanguages());
            return Mono.just("submit");
        });
    }

    @PostMapping("/problem/{name}/submit")
    @PreAuthorize("hasRole('ROLE_USER')")
    public Mono<String> postProblemSubmitRoute(@PathVariable String name, @Valid SubmitRequest form, BindingResult result, Authentication auth) {
        form.setLanguage(JudgeLanguage.prettyToName(form.getLanguage()));

        return problemService.findProblemByName(name).flatMap(p -> {
            if (p == null) return Mono.just("404");
            if (result.hasErrors()) return Mono.just("redirect:/error"); // TODO

            String id = gradeSubmission(p, form, userService.findUserByHandle(auth.getName()).block());
            return Mono.just("redirect:/submission/" + id); // TODO
        });
    }

    private String gradeSubmission(Problem problem, SubmitRequest form, User user) {
        SmoothieRunner.TestSolutionRequest req = SmoothieRunner.TestSolutionRequest.newBuilder()
                .setTestBatchEvenIfFailed(false)
                .setCancelTesting(false)
                .setSolution(SmoothieRunner.Solution.newBuilder()
                        .setCode(form.code)
                        .setLanguage(form.language)
                        .setProblem(problem.getGRPCObject(form.language))
                        .build())
                .build();

        Submission sub = new Submission();
        sub.setId(ObjectId.get().toString());
        sub.setLang(form.language);
        sub.setUserId(user.getId());
        sub.setProblemId(problem.getId());
        sub.setCode(form.code);
        sub.setTimeSubmitted(System.currentTimeMillis() / 1000L);
        sub.setBatchCases(problem.getSubmissionBatchCases());
        sub.setJudgingCompleted(false);

        submissionService.saveSubmission(sub).block();
        runnerService.grade(req, sub);

        return sub.getId();
    }

}
