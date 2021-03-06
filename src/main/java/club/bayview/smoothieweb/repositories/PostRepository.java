package club.bayview.smoothieweb.repositories;

import club.bayview.smoothieweb.models.Post;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface PostRepository extends ReactiveMongoRepository<Post, String> {

    Flux<Post> findByGlobalScopeOrderByCreatedDesc(boolean globalScope);

    Flux<Post> findByUserGroupIdOrderByCreatedDesc(String userGroupId);

}
