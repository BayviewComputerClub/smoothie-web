package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.services.SmoothieSubmissionService;
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

    @GetMapping("/submission/{submissionId}")
    public Mono<String> routeGetSubmission(@PathVariable String submissionId, Model model) {
        return submissionService.findSubmissionById(submissionId).flatMap(submission -> {
            if (submission == null) return Mono.just("404");

            model.addAttribute("submission", submission);
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
