package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.Contest;
import club.bayview.smoothieweb.services.SmoothieContestService;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.util.ErrorCommon;
import club.bayview.smoothieweb.util.NoPermissionException;
import club.bayview.smoothieweb.util.NotFoundException;
import club.bayview.smoothieweb.util.PageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

@Controller
public class ProblemController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    SmoothieProblemService problemService;

    @Autowired
    SmoothieContestService contestService;

    @RequestMapping("/problems")
    public Mono<String> getProblemsRoute(Model model,
                                         Authentication auth,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = PageUtil.DEFAULT_PAGE_SIZE) int pageSize) {
        Pageable p = PageUtil.createPageable(page, pageSize, false, "prettyName", model);

        return Mono.zip(problemService.countProblems(),
                problemService.findProblems(p).filter(pp -> pp.hasPermissionToView(auth)).collectList())
                .flatMap(t -> {
                    model.addAttribute("problems", t.getT2());
                    model.addAttribute(PageUtil.NUM_OF_ENTRIES, t.getT1());
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
