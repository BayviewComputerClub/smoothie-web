package club.bayview.smoothieweb.models;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface UserGroupRepository extends ReactiveMongoRepository<User, String> {
}
