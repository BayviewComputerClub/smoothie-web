package club.bayview.smoothieweb.api.controllers;

import club.bayview.smoothieweb.api.models.APIProblem;
import club.bayview.smoothieweb.services.SmoothieContestService;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.util.ErrorCommon;
import club.bayview.smoothieweb.util.NoPermissionException;
import club.bayview.smoothieweb.util.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
public class APIProblemController {
    @Autowired
    SmoothieProblemService problemService;

    @Autowired
    SmoothieContestService contestService;

    @RequestMapping("/api/v1/problems")
    public Flux<APIProblem> getProblems() {
        return problemService.findProblems(Pageable.unpaged())
                .map(APIProblem::fromProblem);
    }

    @GetMapping("/api/v1/problems/{name}")
    public Mono<APIProblem> getProblemRoute(@PathVariable String name, Authentication auth) {
        return problemService.findProblemByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .map(APIProblem::fromProblem);
    }
}
