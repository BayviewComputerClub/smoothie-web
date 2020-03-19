package club.bayview.smoothieweb.api.controllers;

import club.bayview.smoothieweb.api.models.APIProblem;
import club.bayview.smoothieweb.controllers.JudgeController;
import club.bayview.smoothieweb.models.*;
import club.bayview.smoothieweb.services.*;
import club.bayview.smoothieweb.util.ErrorCommon;
import club.bayview.smoothieweb.util.NoPermissionException;
import club.bayview.smoothieweb.util.NotFoundException;
import com.google.gson.JsonObject;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
public class APIProblemController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    SmoothieProblemService problemService;

    @Autowired
    SmoothieContestService contestService;

    @Autowired
    private SmoothieUserService userService;

    @Autowired
    private SmoothieSubmissionService submissionService;

    @Autowired
    private SmoothieQueuedSubmissionService queuedSubmissionService;

    @RequestMapping("/api/v1/problems")
    public Flux<APIProblem> getProblems() {
        return problemService.findProblems(Pageable.unpaged())
                .map(APIProblem::fromProblem);
    }

    @GetMapping("/api/v1/problems/{name}")
    public Mono<APIProblem> getProblemRoute(@PathVariable String name, Authentication auth) {
        return problemService.findProblemByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .map(APIProblem::fromProblem);
    }

    @PostMapping("/api/v1/problem/{name}/submit")
    @PreAuthorize("hasRole('ROLE_USER')")
    public Mono<String> postProblemSubmission(@PathVariable String name, Authentication auth, @RequestBody Mono<ProblemSubmission> problemSubmission) {
        System.out.println(problemSubmission.block().code);
        return Mono.zip(problemService.findProblemByName(name), userService.findUserByHandle(auth.getName()))
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(t -> {
                    if (!t.getT1().hasPermissionToView(auth))
                        return Mono.error(new NoPermissionException());

                    return gradeSubmission(t.getT1(), t.getT2(), problemSubmission.block());
                })
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "POST /problem/{name}/submit route exception: "));
    }

    private static class ProblemSubmission {
        String lang;
        String code;
    }

    private Mono<String> gradeSubmission(Problem problem, User user, ProblemSubmission problemSubmission) {
        Submission sub = new Submission();
        sub.setId(ObjectId.get().toString());
        sub.setLang(problemSubmission.lang);
        sub.setUserId(user.getId());
        sub.setProblemId(problem.getId());
        sub.setCode(problemSubmission.code);
        sub.setTimeSubmitted(System.currentTimeMillis());
        sub.setJudgingCompleted(false);
        sub.setPoints(0);
        sub.setMaxPoints(problem.getTotalPointsWorth());

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
