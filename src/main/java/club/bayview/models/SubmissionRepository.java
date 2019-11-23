package club.bayview.models;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubmissionRepository extends ReactiveCrudRepository<Submission, String> {

}
