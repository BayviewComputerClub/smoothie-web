package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.*;
import club.bayview.smoothieweb.services.*;
import club.bayview.smoothieweb.util.ErrorCommon;
import club.bayview.smoothieweb.util.NoPermissionException;
import club.bayview.smoothieweb.util.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import javax.validation.constraints.NotNull;

@Controller
public class JudgeController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SmoothieProblemService problemService;

    @Autowired
    private SmoothieSubmissionService submissionService;

    @Autowired
    private SmoothieUserService userService;

    @Autowired
    private SmoothieQueuedSubmissionService queuedSubmissionService;

    @Autowired
    private SmoothieContestService contestService;

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
    public Mono<String> getProblemSubmitRoute(@PathVariable String name, Model model, Authentication auth) {
        return problemService.findProblemByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(p -> {
                    if (!p.hasPermissionToView(auth))
                        return Mono.error(new NoPermissionException());

                    model.addAttribute("problem", p);
                    model.addAttribute("submitRequest", new SubmitRequest());
                    model.addAttribute("languages", JudgeLanguage.getLanguages());
                    model.addAttribute("postUrl", "/problem/" + p.getName() + "/submit");
                    return Mono.just("submit");
                })
                .onErrorResume(e -> ErrorCommon.handle404(e, logger, "GET /problem/{name}/submit route exception: "));
    }

    @GetMapping("/contest/{contestName}/problem/{problemNum}/submit")
    @PreAuthorize("hasRole('ROLE_USER')")
    public Mono<String> getContestProblemSubmitRoute(@PathVariable String contestName, @PathVariable int problemNum, Model model, Authentication auth) {
        return contestService.findContestByName(contestName)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(contest -> {
                    if (!contest.hasPermissionToSubmit(auth))
                        return Mono.error(new NoPermissionException());

                    if (contest.getContestProblemsInOrder().size() <= problemNum)
                        return Mono.error(new NotFoundException());

                    Contest.ContestProblem cp = contest.getContestProblemsInOrder().get(problemNum);
                    model.addAttribute("contestProblem", cp);

                    return problemService.findProblemById(cp.getProblemId());
                })
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(problem -> {
                    model.addAttribute("problem", problem);
                    model.addAttribute("submitRequest", new SubmitRequest());
                    model.addAttribute("languages", JudgeLanguage.getLanguages());
                    model.addAttribute("postUrl", "/contest/" + contestName + "/problem/" + problemNum + "/submit");

                    return Mono.just("submit");
                })
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "GET /problem/{contestName}/problem/{problemName}/submit route exception:"));
    }

    // TODO resubmit for contest needs contest id
    @GetMapping("/submission/{submissionId}/resubmit")
    @PreAuthorize("hasRole('ROLE_USER')")
    public Mono<String> getSubmissionResubmitRoute(@PathVariable String submissionId, Model model, Authentication auth) {
        return submissionService.findSubmissionById(submissionId)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(s -> Mono.zip(problemService.findProblemById(s.getProblemId()), Mono.just(s)))
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(t -> {
                    Problem p = t.getT1();
                    Submission s = t.getT2();
                    // check user permission first
                    if (!s.hasPermissionToView(auth, p) || !p.hasPermissionToView(auth))
                        return Mono.just("no");

                    model.addAttribute("problem", p);
                    model.addAttribute("submitRequest", new SubmitRequest(s.getCode(), JudgeLanguage.nameToPretty(s.getLang())));
                    model.addAttribute("languages", JudgeLanguage.getLanguages());
                    model.addAttribute("postUrl", "/problem/" + p.getName() + "/submit"); // TODO check if authentication is in contest
                    return Mono.just("submit");
                })
                .onErrorResume(e -> ErrorCommon.handle404(e, logger, "GET /submission/{submissionId}/resubmit route exception: "));
    }

    @PostMapping("/problem/{name}/submit")
    @PreAuthorize("hasRole('ROLE_USER')")
    public Mono<String> postProblemSubmitRoute(@PathVariable String name, @Valid SubmitRequest form, BindingResult result, Authentication auth) {
        form.setLanguage(JudgeLanguage.prettyToName(form.getLanguage()));

        if (result.hasErrors())
            return Mono.just("redirect:/error");

        return Mono.zip(problemService.findProblemByName(name), userService.findUserByHandle(auth.getName()))
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(t -> {
                    if (!t.getT1().hasPermissionToView(auth))
                        return Mono.error(new NoPermissionException());

                    return gradeSubmission(t.getT1(), t.getT2(), form);
                })
                .flatMap(id -> Mono.just("redirect:/submission/" + id))
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "POST /problem/{name}/submit route exception: "));
    }

    @PostMapping("/contest/{contestName}/problem/{problemNum}/submit")
    @PreAuthorize("hasRole('ROLE_USER')")
    public Mono<String> postContestProblemSubmitRoute(@PathVariable String contestName, @PathVariable int problemNum, @Valid SubmitRequest form, BindingResult result, Authentication auth) {
        form.setLanguage(JudgeLanguage.prettyToName(form.getLanguage()));
        if (result.hasErrors())
            return Mono.just("redirect:/error");

        return contestService.findContestByName(contestName)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(c -> {
                    if (!c.hasPermissionToSubmit(auth))
                        return Mono.error(new NoPermissionException());

                    Contest.ContestProblem cp = c.getContestProblemsInOrder().get(problemNum);
                    if (cp == null)
                        return Mono.error(new NotFoundException());

                    return Mono.zip(problemService.findProblemById(cp.getProblemId()), userService.findUserByHandle(auth.getName()), Mono.just(c));
                })
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(t -> gradeSubmission(t.getT1(), t.getT2(), t.getT3(), form))
                .flatMap(id -> Mono.just("redirect:/submission/" + id))
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "POST /contest/{contestName}/problem/{problemNum} route exception: "));
    }

    public Mono<String> gradeSubmission(Problem problem, User user, SubmitRequest form) {
        return gradeSubmission(problem, user, null, form);
    }

    public Mono<String> gradeSubmission(Problem problem, User user, Contest contest, SubmitRequest form) {

        Submission sub = new Submission();
        sub.setId(ObjectId.get().toString());
        sub.setLang(form.language);
        sub.setUserId(user.getId());
        sub.setProblemId(problem.getId());
        sub.setCode(form.code);
        sub.setTimeSubmitted(System.currentTimeMillis());
        sub.setJudgingCompleted(false);
        sub.setPoints(0);
        sub.setMaxPoints(problem.getTotalPointsWorth());
        sub.setStatus(Submission.SubmissionStatus.AWAITING_RUNNER);
        if (contest != null) sub.setContestId(contest.getId());

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
