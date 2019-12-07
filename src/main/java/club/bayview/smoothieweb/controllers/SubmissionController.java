package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.services.SmoothieSubmissionService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
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
            model.addAttribute("user", userService.findById(submission.getUserId()).block());
            model.addAttribute("problem", problemService.findProblemById(submission.getProblemId()).block());

            return Mono.just("submission");
        });
    }

    @GetMapping("/user/{handle}/submissions")
    public String getSubmissionsRoute(@PathVariable String handle, Model model) {
        return "submissions";
    }

    @GetMapping("/problem/{name}/submissions")
    public String getProblemSubmissionsRoute(@PathVariable String name, Model model) {
        return "submissions";
    }


}
