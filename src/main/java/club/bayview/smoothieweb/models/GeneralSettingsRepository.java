package club.bayview.smoothieweb.models;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeneralSettingsRepository extends ReactiveMongoRepository<GeneralSettings, String> {

}
