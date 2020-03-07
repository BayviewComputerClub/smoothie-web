package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.Contest;
import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.services.SmoothieContestService;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.services.SmoothieSubmissionService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import club.bayview.smoothieweb.util.ErrorCommon;
import club.bayview.smoothieweb.util.NoPermissionException;
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

    @Autowired
    private SmoothieContestService contestService;

    private Mono<String> getSubmissionHelper(String submissionId, Model model, Authentication auth, String submissionPageTemplate) {
        return submissionService.findSubmissionById(submissionId)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(submission -> Mono.zip(
                        userService.findUserById(submission.getUserId()),
                        problemService.findProblemById(submission.getProblemId()),
                        Mono.just(submission)
                        )
                )
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .doOnNext(tuple -> {
                    model.addAttribute("user", tuple.getT1());
                    model.addAttribute("problem", tuple.getT2());
                    model.addAttribute("submission", tuple.getT3());
                })
                .flatMap(tuple -> {
                    // has permission to view submission
                    if (!tuple.getT3().hasPermissionToView(auth, tuple.getT2())) {
                        return Mono.error(new NoPermissionException());
                    }

                    // if this submission is associated with a contest
                    if (tuple.getT3().getContestId() != null) {
                        return contestService.findContestById(tuple.getT3().getContestId())
                                .switchIfEmpty(Mono.error(new NotFoundException()))
                                .doOnNext(c -> {
                                    model.addAttribute("contest", c);
                                    model.addAttribute("contestProblem", c.getContestProblems().get(tuple.getT3().getProblemId()));
                                });
                    }
                    return Mono.empty();
                })
                .then(Mono.just(submissionPageTemplate))
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "Issue with get submission request: "));
    }

    @GetMapping("/submission/{submissionId}")
    public Mono<String> getSubmissionRoute(@PathVariable String submissionId, Model model, Authentication auth) {
        return getSubmissionHelper(submissionId, model, auth, "submission");
    }

    @GetMapping("/submission/{submissionId}/code")
    public Mono<String> getSubmissionCodeRoute(@PathVariable String submissionId, Model model, Authentication auth) {
        return getSubmissionHelper(submissionId, model, auth, "submission-code");
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
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "GET /user/{handle}/submissions route exception: "));
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
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "GET /problem/{name}/submissions route exception: "));
    }

    @GetMapping("/contest/{contestName}/submissions")
    public Mono<String> getContestSubmissionsRoute(@PathVariable String contestName, Model model, Authentication auth) {
        return contestService.findContestByName(contestName)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .doOnNext(c -> model.addAttribute("contest", c))
                .flatMap(c -> {
                    if (!c.hasPermissionToView(auth))
                        return Mono.error(new NoPermissionException());

                    model.addAttribute("problems", c.getContestProblems());

                    User u = (User) auth.getPrincipal();
                    // check if user can see all submissions, or only submissions by itself
                    if (System.currentTimeMillis() > c.getTimeEnd() || u.isAdmin() || c.getJuryUserIds().contains(u.getId())) {
                        return submissionService.findSubmissionsForContest(c.getId()).collectList();
                    } else {
                        return submissionService.findSubmissionsByUserForContest(u.getId(), c.getId()).collectList();
                    }
                })
                .flatMap(submissions -> {
                    model.addAttribute("submissions", submissions);

                    List<String> userIds = new ArrayList<>();
                    submissions.forEach(s -> userIds.add(s.getUserId()));
                    return userService.findUsersWithIds(userIds).collectList();
                })
                .flatMap(users -> {
                    HashMap<String, String> usersMap = new HashMap<>();
                    users.forEach(user -> usersMap.put(user.getId(), user.getHandle()));
                    model.addAttribute("users", usersMap);

                    return Mono.just("submissions-contest");
                })
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "GET /contest/{contestName}/submissions route exception: "));
    }

    @GetMapping("/contest/{contestName}/problem/{problemNum}/submissions")
    public Mono<String> getContestProblemSubmissionsRoute(@PathVariable String contestName, @PathVariable int problemNum, Model model, Authentication auth) {
        return contestService.findContestByName(contestName)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(c -> {
                    if (!c.hasPermissionToView(auth))
                        return Mono.error(new NoPermissionException());

                    if (problemNum >= c.getContestProblemsInOrder().size())
                        return Mono.error(new NotFoundException());

                    model.addAttribute("contest", c);

                    Contest.ContestProblem cp = c.getContestProblemsInOrder().get(problemNum);
                    model.addAttribute("contestProblem", cp);

                    return Mono.zip(submissionService.findSubmissionsForContestAndProblem(c.getId(), cp.getProblemId()).collectList(), problemService.findProblemById(cp.getProblemId()));
                })
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(t -> {
                    List<Submission> submissions = t.getT1();
                    model.addAttribute("submissions", submissions);
                    model.addAttribute("problem", t.getT2());

                    List<String> ids = new ArrayList<>();
                    submissions.forEach(s -> ids.add(s.getUserId()));
                    return userService.findUsersWithIds(ids).collectList();
                })
                .flatMap(users -> {
                    HashMap<String, String> userMap = new HashMap<>();
                    users.forEach(user -> userMap.put(user.getId(), user.getHandle()));
                    model.addAttribute("users", userMap);

                    return Mono.just("submissions-problem");
                })
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "GET /contest/{contestName}/problem/{problemNum}/submissions route exception: "));
    }


}
