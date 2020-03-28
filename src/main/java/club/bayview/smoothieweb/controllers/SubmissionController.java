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
import club.bayview.smoothieweb.util.PageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Controller
public class SubmissionController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    SmoothieSubmissionService submissionService;

    @Autowired
    SmoothieUserService userService;

    @Autowired
    SmoothieProblemService problemService;

    @Autowired
    SmoothieContestService contestService;

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
                    if (!tuple.getT3().hasPermissionToView(auth, tuple.getT2()))
                        return Mono.error(new NoPermissionException());

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

    private Pageable pageHelper(int page, int pageSize, boolean descending, Model model) {
        model.addAttribute("paramDescending", descending);
        return PageUtil.createPageable(page, pageSize, descending, "timeSubmitted", model);
    }

    @GetMapping("/user/{handle}/submissions")
    public Mono<String> getUserSubmissionsRoute(@PathVariable String handle,
                                                Model model,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = PageUtil.DEFAULT_PAGE_SIZE) int pageSize,
                                                @RequestParam(defaultValue = "true") boolean descending) {
        Pageable pageable = pageHelper(page, pageSize, descending, model);

        return userService.findUserByHandle(handle)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(user -> {
                    model.addAttribute("user", user);
                    return Mono.zip(submissionService.countSubmissionsByUser(user.getId()),
                            submissionService.findSubmissionsByUser(user.getId(), pageable).collectList());
                }).flatMap(t -> {
                    model.addAttribute("submissions", t.getT2());
                    model.addAttribute(PageUtil.NUM_OF_ENTRIES, t.getT1());
                    return problemService.getProblemIdToProblemMap(Flux.fromIterable(t.getT2()).map(Submission::getProblemId));
                }).flatMap(problemsMap -> {
                    model.addAttribute("problems", problemsMap);
                    return Mono.just("submissions-user");
                })
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "GET /user/{handle}/submissions route exception: "));
    }

    @GetMapping("/problem/{name}/submissions")
    public Mono<String> getProblemSubmissionsRoute(@PathVariable String name,
                                                   Model model,
                                                   Authentication auth,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = PageUtil.DEFAULT_PAGE_SIZE) int pageSize,
                                                   @RequestParam(defaultValue = "true") boolean descending) {
        Pageable pageable = pageHelper(page, pageSize, descending, model);

        return problemService.findProblemByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(p -> {
                    if (!p.hasPermissionToView(auth))
                        return Mono.error(new NoPermissionException());

                    model.addAttribute("problem", p);
                    return Mono.zip(submissionService.countSubmissionsByProblem(p.getId()),
                            submissionService.findSubmissionsByProblem(p.getId(), pageable).collectList());
                }).flatMap(t -> {
                    model.addAttribute("submissions", t.getT2());
                    model.addAttribute(PageUtil.NUM_OF_ENTRIES, t.getT1());
                    return userService.getUserIdToUserMap(Flux.fromIterable(t.getT2()).map(Submission::getUserId));
                }).flatMap(usersMap -> {
                    model.addAttribute("users", usersMap);
                    return Mono.just("submissions-problem");
                })
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "GET /problem/{name}/submissions route exception: "));
    }

    @GetMapping("/contest/{contestName}/submissions")
    public Mono<String> getContestSubmissionsRoute(@PathVariable String contestName,
                                                   Model model,
                                                   Authentication auth,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = PageUtil.DEFAULT_PAGE_SIZE) int pageSize,
                                                   @RequestParam(defaultValue = "true") boolean descending) {
        Pageable pageable = pageHelper(page, pageSize, descending, model);

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
                        return Mono.zip(submissionService.countSubmissionsForContest(c.getId()),
                                submissionService.findSubmissionsForContest(c.getId(), pageable).collectList());
                    } else {
                        return Mono.zip(submissionService.countSubmissionsByUserForContest(u.getId(), c.getId()),
                                submissionService.findSubmissionsByUserForContest(u.getId(), c.getId(), pageable).collectList());
                    }
                })
                .flatMap(t -> {
                    model.addAttribute("submissions", t.getT2());
                    model.addAttribute(PageUtil.NUM_OF_ENTRIES, t.getT1());
                    return Mono.zip(userService.getUserIdToUserMap(Flux.fromIterable(t.getT2()).map(Submission::getUserId)), problemService.getProblemIdToProblemMap(Flux.fromIterable(t.getT2()).map(Submission::getProblemId)));
                })
                .flatMap(t -> {
                    model.addAttribute("users", t.getT1());
                    model.addAttribute("problems", t.getT2());
                    return Mono.just("submissions-contest");
                })
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "GET /contest/{contestName}/submissions route exception: "));
    }

    @GetMapping("/contest/{contestName}/problem/{problemNum}/submissions")
    public Mono<String> getContestProblemSubmissionsRoute(@PathVariable String contestName,
                                                          @PathVariable int problemNum,
                                                          Model model,
                                                          Authentication auth,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = PageUtil.DEFAULT_PAGE_SIZE) int pageSize,
                                                          @RequestParam(defaultValue = "true") boolean descending) {

        Pageable pageable = pageHelper(page, pageSize, descending, model);

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

                    return Mono.zip(submissionService.findSubmissionsForContestAndProblem(c.getId(), cp.getProblemId(), pageable).collectList(),
                            problemService.findProblemById(cp.getProblemId()),
                            submissionService.countSubmissionsForContestAndProblem(c.getId(), cp.getProblemId()));
                })
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(t -> {
                    model.addAttribute("submissions", t.getT1());
                    model.addAttribute("problem", t.getT2());
                    model.addAttribute(PageUtil.NUM_OF_ENTRIES, t.getT3());

                    return userService.getUserIdToUserMap(Flux.fromIterable(t.getT1()).map(Submission::getUserId));
                })
                .flatMap(userMap -> {
                    model.addAttribute("users", userMap);
                    return Mono.just("submissions-problem");
                })
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "GET /contest/{contestName}/problem/{problemNum}/submissions route exception: "));
    }
}
