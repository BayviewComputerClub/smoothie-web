package club.bayview.controllers;

import club.bayview.models.Problem;
import club.bayview.models.Submission;
import club.bayview.services.SmoothieProblemService;
import club.bayview.services.SmoothieSubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

@Controller
public class JudgeController {

    @Autowired
    private SmoothieProblemService problemService;

    @Autowired
    private SmoothieSubmissionService submissionService;

    @GetMapping("/problem/{problemName}")
    public String routeGetProblem(@PathVariable String problemName, Model model) {
        Mono<Problem> problem = problemService.findProblemByName(problemName);
        model.addAttribute("problem", problem);
        return "problem";
    }

    @GetMapping("/submission/{submissionId}")
    public String routeGetSubmission(@PathVariable String submissionId, Model model) {
        Mono<Submission> submission = submissionService.findSubmissionById(submissionId);
        model.addAttribute("submission", submission);
        return "submission";
    }

}
