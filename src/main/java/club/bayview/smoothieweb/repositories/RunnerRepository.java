package club.bayview.smoothieweb.repositories;

import club.bayview.smoothieweb.models.Runner;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface RunnerRepository extends ReactiveMongoRepository<Runner, String> {

    Mono<Runner> findByName(String name);

}
