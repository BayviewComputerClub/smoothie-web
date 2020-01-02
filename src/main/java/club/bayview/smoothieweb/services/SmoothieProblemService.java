package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.*;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
public class SmoothieProblemService {

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private TestDataRepository testDataRepository;

    public Mono<Problem> findProblemById(String id) {
        return problemRepository.findById(id);
    }

    public Mono<Problem> findProblemByName(String name) {
        return problemRepository.findByName(name);
    }

    public Flux<Problem> findProblems() {
        return problemRepository.findAll();
    }

    public Flux<Problem> findProblemsWithIds(List<String> ids) {
        return problemRepository.findAllByIdIn(ids);
    }

    public Mono<String> findProblemTestDataHash(String id) throws Exception {
        return testDataRepository.getTestDataHash(id);
    }

    public Mono<TestData> findProblemTestData(String id) throws Exception {
        //System.out.println(testDataRepository.getTestData(id).block().toString()); debug
        return testDataRepository.getTestData(id);
    }

    public Mono<Problem> saveTestDataForProblem(TestData data, Problem problem) throws Exception {
        return testDataRepository.addTestData(data, problem.getId()).flatMap(objectId -> {
            problem.setTestDataId(objectId.toString());
            return saveProblem(problem);
        });
    }

    public Mono<Problem> saveProblem(Problem problem) {
        return problemRepository.save(problem);
    }
}
