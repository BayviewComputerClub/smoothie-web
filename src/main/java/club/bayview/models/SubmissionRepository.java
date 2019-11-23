package club.bayview.models;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    @Override
    void delete(Submission privilege);
}
