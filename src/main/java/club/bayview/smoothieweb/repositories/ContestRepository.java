package club.bayview.smoothieweb.repositories;

import club.bayview.smoothieweb.models.Contest;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface ContestRepository extends ReactiveMongoRepository<Contest, String> {

    Mono<Contest> findByName(String name);

}
