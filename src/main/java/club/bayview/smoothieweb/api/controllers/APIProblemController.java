package club.bayview.smoothieweb.api.controllers;

import club.bayview.smoothieweb.api.models.APIProblem;
import club.bayview.smoothieweb.models.GeneralSettings;
import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.services.SmoothieContestService;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class APIProblemController {
    @Autowired
    SmoothieProblemService problemService;

    @Autowired
    SmoothieContestService contestService;

    @RequestMapping("/api/v1/problems")
    public Flux<APIProblem> getProblems() {
        return problemService.findProblemsAlphaDesc()
                .map(APIProblem::fromProblem);
    }
}
