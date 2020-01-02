package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.services.SmoothieSubmissionService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private SmoothieSubmissionService submissionService;

    @Autowired
    private SmoothieUserService userService;

    @Autowired
    private SmoothieProblemService problemService;

    @GetMapping("/submission/{submissionId}")
    // TODO make sure you have permission
    public Mono<String> routeGetSubmission(@PathVariable String submissionId, Model model) {
        return submissionService.findSubmissionById(submissionId).flatMap(submission -> {
            if (submission == null) return Mono.just("404");

            model.addAttribute("submission", submission);

            return Mono.zip(userService.findUserById(submission.getUserId()), problemService.findProblemById(submission.getProblemId())).flatMap(tuple -> {
                model.addAttribute("user", tuple.getT1());
                model.addAttribute("problem", tuple.getT2());
                return Mono.just("submission");
            });
        });
    }

    @GetMapping("/submission/{submissionId}/code")
    // TODO make sure you have permission
    public Mono<String> routeGetSubmissionCode(@PathVariable String submissionId, Model model) {
        return submissionService.findSubmissionById(submissionId).flatMap(submission -> {
            if (submission == null) return Mono.just("404");

            model.addAttribute("submission", submission);

            return Mono.zip(userService.findUserById(submission.getUserId()), problemService.findProblemById(submission.getProblemId())).flatMap(tuple -> {
                model.addAttribute("user", tuple.getT1());
                model.addAttribute("problem", tuple.getT2());
                return Mono.just("submission-code");
            });
        });
    }

    @GetMapping("/user/{handle}/submissions")
    public Mono<String> getSubmissionsRoute(@PathVariable String handle, Model model) {
        return userService.findUserByHandle(handle).flatMap(user -> {
            if (user == null) return Mono.just("404");

            model.addAttribute("user", user);
            return submissionService.findSubmissionsByUser(user.getId()).collectList();
        }).flatMap(submissions -> {
            if (submissions.equals("404")) return Mono.just("404");

            List<String> ids = new ArrayList<>();
            ((List<Submission>) submissions).forEach(s -> ids.add(s.getProblemId()));

            Collections.reverse(ids);

            model.addAttribute("submissions", submissions);
            return problemService.findProblemsWithIds(ids).collectList();
        }).flatMap(problems -> {
            if (problems.equals("404")) return Mono.just("404");

            HashMap<String, Problem> problemsMap = new HashMap<>();
            ((List<Problem>) problems).forEach(problem -> problemsMap.put(problem.getId(), problem));
            model.addAttribute("problems", problemsMap);
            return Mono.just("submissions-user");
        });
    }

    @GetMapping("/problem/{name}/submissions")
    public Mono<String> getProblemSubmissionsRoute(@PathVariable String name, Model model) {
        return problemService.findProblemByName(name).flatMap(p -> {
            if (p == null) return Mono.just("404");

            model.addAttribute("problem", p);
            return submissionService.findSubmissionsByProblem(p.getId()).collectList();
        }).flatMap(submissions -> {
            if (submissions.equals("404") || !(submissions instanceof List)) return Mono.just("404");
            List<String> ids = new ArrayList<>();
            ((List<Submission>) submissions).forEach(s -> ids.add(s.getUserId()));

            Collections.reverse(ids);

            model.addAttribute("submissions", submissions);
            return userService.findUsersWithIds(ids).collectList();
        }).flatMap(users -> {
            if (users.equals("404")) return Mono.just("404");

            HashMap<String, String> usersMap = new HashMap<>();
            ((List<User>) users).forEach(user -> usersMap.put(user.getId(), user.getHandle()));
            model.addAttribute("users", usersMap);
            return Mono.just("submissions-problem");
        });
    }


}
