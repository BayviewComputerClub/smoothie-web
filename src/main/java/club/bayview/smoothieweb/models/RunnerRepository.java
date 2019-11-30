package club.bayview.smoothieweb.models;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface RunnerRepository extends ReactiveMongoRepository<Runner, String> {

    Mono<Runner> findByName(String name);

}
