package club.bayview.models;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    @Override
    void delete(Problem privilege);
}
