package club.bayview.smoothieweb.models;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface CommentRepository extends ReactiveMongoRepository<Post, String> {

}
