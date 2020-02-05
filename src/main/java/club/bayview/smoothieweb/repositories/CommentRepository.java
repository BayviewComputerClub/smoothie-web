package club.bayview.smoothieweb.repositories;

import club.bayview.smoothieweb.models.Post;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface CommentRepository extends ReactiveMongoRepository<Post, String> {

}
