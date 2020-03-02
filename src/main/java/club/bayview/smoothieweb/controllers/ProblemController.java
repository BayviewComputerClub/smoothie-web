package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.Contest;
import club.bayview.smoothieweb.services.SmoothieContestService;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.util.ErrorCommon;
import club.bayview.smoothieweb.util.NoPermissionException;
import club.bayview.smoothieweb.util.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@Controller
public class ProblemController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    SmoothieProblemService problemService;

    @Autowired
    SmoothieContestService contestService;

    @RequestMapping("/problems")
    public Mono<String> getProblemsRoute(Model model) {
        return problemService.findProblemsAlphaDesc().collectList().flatMap(problems -> {
            model.addAttribute("problems", problems);

            return Mono.just("problems");
        });
    }

    @RequestMapping("/contest/{contestName}/problems")
    public Mono<String> getContestProblemsRoute(@PathVariable String contestName, Model model, Authentication auth) {
        return contestService.findContestByName(contestName)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(c -> {
                    if (!c.hasPermissionToViewProblems(auth))
                        return Mono.error(new NoPermissionException());

                    model.addAttribute("contest", c);
                    return Mono.just("contest-problems");
                })
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "GET /contest/{contestName}/problems"));
    }

    @GetMapping("/problem/{name}")
    public Mono<String> getProblemRoute(@PathVariable String name, Model model, Authentication auth) {
        return problemService.findProblemByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(p -> {
                    if (!p.hasPermissionToView(auth))
                        return Mono.error(new NoPermissionException());
                    
                    model.addAttribute("problem", p);
                    return Mono.just("problem");
                })
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "GET /problem/{name} route exception: "));
    }

    @GetMapping("/contest/{contestName}/problem/{problemNum}")
    public Mono<String> getContestProblemRoute(@PathVariable String contestName, @PathVariable int problemNum, Model model, Authentication auth) {

        return contestService.findContestByName(contestName)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(c -> {
                    if (!c.hasPermissionToViewProblems(auth))
                        return Mono.error(new NoPermissionException());

                    if (problemNum >= c.getContestProblems().size()) {
                        return Mono.error(new NotFoundException());
                    }
                    Contest.ContestProblem cp = c.getContestProblemsInOrder().get(problemNum);

                    // add contest problem (override some problem fields)
                    model.addAttribute("contestProblem", cp);
                    model.addAttribute("contest", c);
                    return problemService.findProblemById(cp.getProblemId()).switchIfEmpty(Mono.error(new NotFoundException()));
                })
                .flatMap(problem -> {
                    // add original problem
                    model.addAttribute("problem", problem);
                    return Mono.just("problem");
                })
                .onErrorResume(e -> ErrorCommon.handleBasic(e, logger, "GET /contest/{contestName}/{problemNum} route exception: "));
    }

}
