package club.bayview.smoothieweb.repositories;

import club.bayview.smoothieweb.models.GeneralSettings;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeneralSettingsRepository extends ReactiveMongoRepository<GeneralSettings, String> {

}
