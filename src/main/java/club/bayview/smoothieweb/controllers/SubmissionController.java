package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.services.SmoothieSubmissionService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import club.bayview.smoothieweb.util.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Controller
public class SubmissionController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SmoothieSubmissionService submissionService;

    @Autowired
    private SmoothieUserService userService;

    @Autowired
    private SmoothieProblemService problemService;

    private Mono<String> getSubmissionHelper(String submissionId, Model model, String submissionPageTemplate) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return submissionService.findSubmissionById(submissionId)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(submission -> Mono.zip(
                        userService.findUserById(submission.getUserId()),
                        problemService.findProblemById(submission.getProblemId()),
                        Mono.just(submission)
                        )
                )
                .flatMap(tuple -> {
                    // has permission to view submission
                    if (!tuple.getT3().hasPermissionToView(auth, tuple.getT2())) {
                        return Mono.just("no");
                    }

                    model.addAttribute("user", tuple.getT1());
                    model.addAttribute("problem", tuple.getT2());
                    model.addAttribute("submission", tuple.getT3());
                    return Mono.just(submissionPageTemplate);
                })
                .onErrorResume(e -> {
                    if (e instanceof NotFoundException) {
                        return Mono.just("404");
                    } else {
                        logger.error("Issue with get submission request: ", e);
                        return Mono.just("500");
                    }
                });
    }

    @GetMapping("/submission/{submissionId}")
    public Mono<String> getSubmissionRoute(@PathVariable String submissionId, Model model) {
        return getSubmissionHelper(submissionId, model, "submission");
    }

    @GetMapping("/submission/{submissionId}/code")
    public Mono<String> getSubmissionCodeRoute(@PathVariable String submissionId, Model model) {
        return getSubmissionHelper(submissionId, model, "submission-code");
    }

    @GetMapping("/user/{handle}/submissions")
    // TODO remove problems that don't exit
    public Mono<String> getUserSubmissionsRoute(@PathVariable String handle, Model model) {
        return userService.findUserByHandle(handle)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(user -> {
                    model.addAttribute("user", user);
                    return submissionService.findSubmissionsByUser(user.getId()).collectList();
                }).flatMap(submissions -> {
                    List<String> ids = new ArrayList<>();
                    submissions.forEach(s -> ids.add(s.getProblemId()));

                    Collections.reverse(ids);

                    model.addAttribute("submissions", submissions);
                    return problemService.findProblemsWithIds(ids).collectList();
                }).flatMap(problems -> {
                    HashMap<String, Problem> problemsMap = new HashMap<>();
                    problems.forEach(problem -> problemsMap.put(problem.getId(), problem));
                    model.addAttribute("problems", problemsMap);
                    return Mono.just("submissions-user");
                })
                .onErrorResume(e -> Mono.just("404"));
    }

    @GetMapping("/problem/{name}/submissions")
    // TODO remove users that don't exist
    public Mono<String> getProblemSubmissionsRoute(@PathVariable String name, Model model) {
        return problemService.findProblemByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(p -> {
                    model.addAttribute("problem", p);
                    return submissionService.findSubmissionsByProblem(p.getId()).collectList();
                }).flatMap(submissions -> {
                    List<String> ids = new ArrayList<>();
                    submissions.forEach(s -> ids.add(s.getUserId()));

                    Collections.reverse(ids);

                    model.addAttribute("submissions", submissions);
                    return userService.findUsersWithIds(ids).collectList();
                }).flatMap(users -> {
                    HashMap<String, String> usersMap = new HashMap<>();
                    users.forEach(user -> usersMap.put(user.getId(), user.getHandle()));
                    model.addAttribute("users", usersMap);
                    return Mono.just("submissions-problem");
                })
                .onErrorResume(e -> Mono.just("404"));
    }


}
