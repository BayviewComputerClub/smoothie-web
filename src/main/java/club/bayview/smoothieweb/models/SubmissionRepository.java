package club.bayview.smoothieweb.models;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface SubmissionRepository extends ReactiveMongoRepository<Submission, String> {

    Flux<Submission> findByProblemIdIsOrderByTimeSubmittedDesc(String problemId);

    Flux<Submission> findByUserIdIsOrderByTimeSubmittedDesc(String userId);

}
