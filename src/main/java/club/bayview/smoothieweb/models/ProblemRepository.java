package club.bayview.smoothieweb.models;


import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ProblemRepository extends ReactiveMongoRepository<Problem, String> {

    Mono<Problem> findByName(String name);

}
