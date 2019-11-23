package club.bayview.services;

import club.bayview.models.Problem;
import club.bayview.models.ProblemRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SmoothieProblemService {

    @Autowired
    private ProblemRepository problemRepository;

    public Mono<Problem> findProblemById(ObjectId id) {
        return problemRepository.findById(id);
    }

    public Mono<Problem> findProblemByName(String name) {
        return problemRepository.findByName(name);
    }

}
