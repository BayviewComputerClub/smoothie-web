package club.bayview.smoothieweb.repositories;

import club.bayview.smoothieweb.models.Submission;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface SubmissionRepository extends ReactiveMongoRepository<Submission, String> {

    Flux<Submission> findByProblemIdOrderByTimeSubmittedDesc(String problemId);

    Flux<Submission> findByUserIdOrderByTimeSubmittedDesc(String userId);

    Flux<Submission> findByUserIdAndProblemIdOrderByTimeSubmittedDesc(String userId, String problemId);

    Flux<Submission> findByUserIdAndContestIdOrderByTimeSubmittedDesc(String userId, String contestId);

    Flux<Submission> findByUserIdAndContestIdAndProblemIdOrderByTimeSubmittedDesc(String userId, String contestId, String problemId);

    Flux<Submission> findByContestIdOrderByTimeSubmittedDesc(String contestId);

    Flux<Submission> findByContestIdAndProblemIdOrderByTimeSubmittedDesc(String contestId, String problemId);

}
