package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.models.ProblemRepository;
import club.bayview.smoothieweb.models.User;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class SmoothieProblemService {

    @Autowired
    private ProblemRepository problemRepository;

    public Mono<Problem> findProblemById(String id) {
        return problemRepository.findById(id);
    }

    public Mono<Problem> findProblemByName(String name) {
        return problemRepository.findByName(name);
    }

    public Flux<Problem> findProblems() {
        return problemRepository.findAll();
    }

    public void saveProblem(Problem problem) {
        problemRepository.save(problem).block();
    }
}
