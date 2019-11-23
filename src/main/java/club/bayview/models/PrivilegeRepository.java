package club.bayview.models;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface PrivilegeRepository extends ReactiveCrudRepository<Privilege, String> {

    Mono<Privilege> findByName(String name);

}
