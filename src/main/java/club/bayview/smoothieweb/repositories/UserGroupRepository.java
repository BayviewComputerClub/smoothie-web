package club.bayview.smoothieweb.repositories;

import club.bayview.smoothieweb.models.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface UserGroupRepository extends ReactiveMongoRepository<User, String> {
}
