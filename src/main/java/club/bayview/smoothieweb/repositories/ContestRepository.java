package club.bayview.smoothieweb.repositories;

import club.bayview.smoothieweb.models.Contest;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ContestRepository extends ReactiveMongoRepository<Contest, String> {

    Mono<Contest> findByName(String name);

}
