package club.bayview.smoothieweb.repositories;

import club.bayview.smoothieweb.models.Problem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface ProblemRepository extends ReactiveMongoRepository<Problem, String> {

    Mono<Problem> findByName(String name);

    Flux<Problem> findAllBy(Pageable p);

    Mono<Long> countAllBy();

    Flux<Problem> findAllByIdIn(List<String> ids);
    
    Flux<Problem> findAllByIdIn(Flux<String> ids);

    Flux<Problem> findByPrettyNameLikeOrderByTimeCreatedDesc(String query);

    Flux<Problem> findByNameLikeOrderByTimeCreatedDesc(String query);
}
