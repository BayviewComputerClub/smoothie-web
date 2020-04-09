package club.bayview.smoothieweb.repositories;

import club.bayview.smoothieweb.models.QueuedSubmission;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface QueuedSubmissionRepository extends ReactiveMongoRepository<QueuedSubmission, String> {

    Flux<QueuedSubmission> findAllByStatusOrderByTimeRequestedAsc(QueuedSubmission.QueuedSubmissionStatus status);

    Flux<QueuedSubmission> findAllByOrderByTimeRequestedAsc();

    Mono<Long> deleteAllById(String id);
}
