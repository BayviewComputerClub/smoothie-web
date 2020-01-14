package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.*;
import club.bayview.smoothieweb.services.*;
import club.bayview.smoothieweb.util.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
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
import java.util.concurrent.atomic.AtomicReference;

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

    @Autowired
    private SmoothieQueuedSubmissionService queuedSubmissionService;

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
        return problemService.findProblemByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(p -> {
                    model.addAttribute("problem", p);
                    return Mono.just("problem");
                })
                .onErrorResume(e -> Mono.just("404"));
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    private static class SubmitRequest {
        @NotNull
        private String code;

        @NotNull
        private String language;
    }

    @GetMapping("/problem/{name}/submit")
    @PreAuthorize("hasRole('ROLE_USER')")
    public Mono<String> getProblemSubmitRoute(@PathVariable String name, Model model) {
        return problemService.findProblemByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(p -> {
                    model.addAttribute("problem", p);
                    model.addAttribute("submitRequest", new SubmitRequest());
                    model.addAttribute("languages", JudgeLanguage.getLanguages());
                    model.addAttribute("postUrl", "/problem/" + p.getName() + "/submit");
                    return Mono.just("submit");
                })
                .onErrorResume(e -> Mono.just("404"));
    }

    @GetMapping("/submission/{submissionId}/resubmit")
    @PreAuthorize("hasRole('ROLE_USER')")
    // TODO proper permission for only user that submitted
    public Mono<String> getSubmissionResubmitRoute(@PathVariable String submissionId, Model model) {
        AtomicReference<Submission> submission = new AtomicReference<>();

        return submissionService.findSubmissionById(submissionId)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(s -> {
                    submission.set(s);
                    return problemService.findProblemById(s.getProblemId());
                })
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(p -> {
                    model.addAttribute("problem", p);
                    model.addAttribute("submitRequest", new SubmitRequest(submission.get().getCode(), JudgeLanguage.nameToPretty(submission.get().getLang())));
                    model.addAttribute("languages", JudgeLanguage.getLanguages());
                    model.addAttribute("postUrl", "/problem/" + p.getName() + "/submit");
                    return Mono.just("submit");
                })
                .onErrorResume(e -> Mono.just("404"));
    }

    @PostMapping("/problem/{name}/submit")
    @PreAuthorize("hasRole('ROLE_USER')")
    public Mono<String> postProblemSubmitRoute(@PathVariable String name, @Valid SubmitRequest form, BindingResult result, Authentication auth) {
        form.setLanguage(JudgeLanguage.prettyToName(form.getLanguage()));

        return Mono.zip(problemService.findProblemByName(name), userService.findUserByHandle(auth.getName()))
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(t -> {
                    if (result.hasErrors()) return Mono.just("redirect:/error");
                    return gradeSubmission(t.getT1(), form, t.getT2()).flatMap(id -> Mono.just("redirect:/submission/" + id));
                })
                .onErrorResume(e -> Mono.just("404"));
    }

    private Mono<String> gradeSubmission(Problem problem, SubmitRequest form, User user) {

        Submission sub = new Submission();
        sub.setId(ObjectId.get().toString());
        sub.setLang(form.language);
        sub.setUserId(user.getId());
        sub.setProblemId(problem.getId());
        sub.setCode(form.code);
        sub.setTimeSubmitted(System.currentTimeMillis() / 1000L);
        sub.setJudgingCompleted(false);

        return problem.getSubmissionBatchCases()
                .flatMap(batches -> {
                    sub.setBatchCases(batches);
                    return submissionService.saveSubmission(sub);
                })
                .flatMap(s -> queuedSubmissionService.saveQueuedSubmission(new QueuedSubmission(s.getId(), problem.getId())))
                .flatMap(q -> {
                    queuedSubmissionService.checkRunnersTask();
                    return Mono.just(q.getSubmissionId());
                });
    }
}
