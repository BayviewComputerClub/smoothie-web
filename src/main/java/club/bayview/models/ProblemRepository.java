package club.bayview.models;


import org.bson.types.ObjectId;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ProblemRepository extends ReactiveCrudRepository<Problem, String> {

    Mono<Problem> findByName(String name);

}
