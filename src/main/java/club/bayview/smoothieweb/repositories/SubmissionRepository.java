package club.bayview.smoothieweb.repositories;

import club.bayview.smoothieweb.models.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SubmissionRepository extends ReactiveMongoRepository<Submission, String> {

    Flux<Submission> findByProblemId(String problemId, Pageable p);

    Mono<Long> countByProblemId(String problemId);

    Flux<Submission> findByUserId(String userId, Pageable p);

    Mono<Long> countByUserId(String userId);

    Flux<Submission> findByUserIdAndProblemId(String userId, String problemId, Pageable p);

    Mono<Long> countByUserIdAndProblemId(String userId, String problemId);

    Flux<Submission> findByUserIdAndContestId(String userId, String contestId);

    Mono<Long> countByUserIdAndContestId(String userId, String contestId);

    Flux<Submission> findByUserIdAndContestIdAndProblemId(String userId, String contestId, String problemId, Pageable p);

    Mono<Long> countByUserIdAndContestIdAndProblemId(String userId, String contestId, String problemId);

    Flux<Submission> findByContestId(String contestId, Pageable p);

    Mono<Long> countByContestId(String contestId);

    Flux<Submission> findByContestIdAndProblemId(String contestId, String problemId, Pageable p);

    Mono<Long> countByContestIdAndProblemId(String contestId, String problemId);

}
