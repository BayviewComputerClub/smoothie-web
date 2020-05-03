package club.bayview.smoothieweb.api.controllers;

import club.bayview.smoothieweb.api.models.APIProblem;
import club.bayview.smoothieweb.models.Contest;
import club.bayview.smoothieweb.services.SmoothieContestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class APIContentController {
    @Autowired
    SmoothieContestService contestService;

    @RequestMapping("/api/v1/contests")
    public Flux<Contest> getProblems() {
        return contestService.findAllContests(Pageable.unpaged());
    }
}
