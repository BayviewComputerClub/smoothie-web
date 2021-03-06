package club.bayview.smoothieweb.repositories;

import club.bayview.smoothieweb.models.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User, String> {

    Mono<User> findByHandle(String handle);

    Mono<User> findByEmail(String email);

    Flux<User> findAllByIdIn(List<String> ids);

    Flux<User> findAllByIdIn(Flux<String> ids);

    Flux<User> findAllByHandleIn(List<String> handles);
}
