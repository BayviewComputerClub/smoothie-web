package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.services.SmoothieSubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class JudgeController {

    @Autowired
    private SmoothieProblemService problemService;

    @Autowired
    private SmoothieSubmissionService submissionService;

    @RequestMapping("/contests")
    public String routeGetContests(Model model) {
        return "contests";
    }

    @RequestMapping("/problems")
    public String routeGetProblems(Model model) {
        model.addAttribute("problems", problemService.findProblems().collectList().block());
        return "problems";
    }

    @GetMapping("/problem/{name}")
    public String routeGetProblem(@PathVariable String name, Model model) {
        Problem p = problemService.findProblemByName(name).block();
        if (p == null) return "404";
        model.addAttribute("problem", p);
        return "problem";
    }

}
