package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.services.SmoothieSubmissionService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

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
    public String getSubmissionsRoute(@PathVariable String handle, Model model) {
        return "submissions";
    }

    @GetMapping("/problem/{name}/submissions")
    public Mono<String> getProblemSubmissionsRoute(@PathVariable String name, Model model) {
        return problemService.findProblemByName(name).flatMap(p -> {
            if (p == null) return Mono.just("404");

            model.addAttribute("submissions", submissionService.findSubmissionsByProblem(p.getId()).collectList().block());
            return Mono.just("submissions");
        });
    }


}
