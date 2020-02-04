package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.models.ProblemRepository;
import club.bayview.smoothieweb.models.TestDataRepository;
import club.bayview.smoothieweb.models.testdata.StoredTestData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    public Mono<byte[]> findRawProblemTestData(String id) throws Exception {
        return testDataRepository.getRawTestData(id);
    }

    public Flux<byte[]> findRawProblemTestDataFlux(String id) throws Exception {
        return testDataRepository.getRawTestDataFlux(id);
    }

    public Mono<StoredTestData.TestData> findProblemTestData(String id) throws Exception {
        return testDataRepository.getTestData(id);
    }

    public Mono<Problem> saveTestDataForProblem(Problem.TestDataWrapper data, Problem problem) throws Exception {
        String id = problem.getTestDataId();
        problem.setProblemBatches(data.getBatches()); // set batch point information

        return testDataRepository.addTestData(data.getTestData(), problem.getId()).flatMap(objectId -> {
            problem.setTestDataId(objectId.toString());

            try {
                return id == null || id.equals("") ? Mono.empty() : testDataRepository.removeTestData(id);
            } catch (Exception e) {
                e.printStackTrace();
                return Mono.empty();
            }
        }).then(saveProblem(problem));
    }

    public Mono<Problem> saveProblem(Problem problem) {
        return problemRepository.save(problem);
    }

    public Mono<Void> deleteProblem(Problem problem) {
        return problemRepository.delete(problem);
    }

    public Mono<Void> deleteProblemById(String id) {
        return problemRepository.deleteById(id);
    }
}
